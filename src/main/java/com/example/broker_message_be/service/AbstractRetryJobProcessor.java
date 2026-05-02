package com.example.broker_message_be.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import com.example.broker_message_be.chain.RetryChainContext;
import com.example.broker_message_be.chain.RetryOperationStep;
import com.example.broker_message_be.chain.RetryStep;
import com.example.broker_message_be.chain.SuccessEmailStep;
import com.example.broker_message_be.chain.UpdateRetryJobStep;
import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.RetryEnvelope;
import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;
import com.example.broker_message_be.entity.RetryJob;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.enumtype.RetryStepName;
import com.example.broker_message_be.gateway.RetryTargetClient;
import com.example.broker_message_be.repository.RetryJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractRetryJobProcessor<T extends RetryPayload, D extends BaseRetryDetailJob>
        implements RetryJobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRetryJobProcessor.class);

    private final ObjectMapper objectMapper;
    private final RetryJobRepository retryJobRepository;
    private final NotificationService notificationService;
    private final BrokerProperties properties;

    protected AbstractRetryJobProcessor(ObjectMapper objectMapper, RetryJobRepository retryJobRepository,
            NotificationService notificationService, BrokerProperties properties) {
        this.objectMapper = objectMapper;
        this.retryJobRepository = retryJobRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Override
    public void handleTopicMessage(String rawMessage, String topic) {
        RetryEnvelope<T> envelope = readEnvelope(rawMessage);
        T payload = requirePayload(envelope);
        validatePayload(payload);
        String jobKey = buildJobKey(payload, rawMessage);
        String requestPayload = extractRequestPayload(rawMessage);

        Optional<RetryJob> existingJob = retryJobRepository.findByJobKey(jobKey);
        if (existingJob.isPresent()) {
            RetryJob retryJob = existingJob.get();
            if (retryJob.getStatus() == RetryExecutionStatus.SUCCESS) {
                logger.info("Duplicate retry job ignored. jobType={}, retryJobId={}, jobKey={}",
                        getJobType(), retryJob.getId(), jobKey);
                return;
            }
            retryJob.setReferenceId(payload.getId());
            retryJob.setNotificationEmail(resolveRecipient(envelope, payload));
            retryJob.setRequestPayload(requestPayload);
            retryJob.setPayload(buildPayloadSnapshot(retryJob));
            retryJob.setNextRetryAt(LocalDateTime.now());
            retryJobRepository.save(retryJob);
            processStoredJob(retryJob.getId());
            return;
        }

        RetryJob retryJob = new RetryJob();
        retryJob.setJobKey(jobKey);
        retryJob.setJobType(getJobType());
        retryJob.setSourceTopic(topic);
        retryJob.setReferenceId(payload.getId());
        retryJob.setNotificationEmail(resolveRecipient(envelope, payload));
        retryJob.initializeForNewJob(LocalDateTime.now());
        retryJob.setRequestPayload(requestPayload);
        retryJob.setPayload(buildPayloadSnapshot(retryJob));
        retryJob = retryJobRepository.save(retryJob);

        processStoredJob(retryJob.getId());
    }

    @Override
    public void processPendingJobs() {
        List<RetryJob> pendingJobs = retryJobRepository
                .findByJobTypeAndStatusInAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByCreatedAtAsc(
                        getJobType(),
                        List.of(RetryExecutionStatus.PENDING, RetryExecutionStatus.ERROR),
                        LocalDateTime.now(),
                        properties.getRetry().getMaxAttempts(),
                        PageRequest.of(0, properties.getRetry().getBatchSize()));

        for (RetryJob retryJob : pendingJobs) {
            processStoredJob(retryJob.getId());
        }
    }

    private void processStoredJob(Long retryJobId) {
        RetryJob retryJob = retryJobRepository.findById(retryJobId)
                .orElseThrow(() -> new IllegalStateException("Retry job no encontrado: " + retryJobId));
        if (retryJob.getStatus() == RetryExecutionStatus.SUCCESS) {
            return;
        }
        if (!retryJob.canAttempt(properties.getRetry().getMaxAttempts())) {
            logger.warn("Retry job reached max attempts. jobType={}, retryJobId={}, retryCount={}, maxAttempts={}",
                    getJobType(), retryJobId, retryJob.getRetryCount(), properties.getRetry().getMaxAttempts());
            return;
        }

        T payload = readPayload(retryJob);
        validatePayload(payload);
        D detailJob = findDetailJob(retryJobId)
                .orElseGet(() -> buildTransientDetailJob(retryJobId, payload));
        retryJob.setLastAttemptAt(LocalDateTime.now());
        persistState(retryJob, detailJob);

        RetryChainContext<T, D> context = new RetryChainContext<>(retryJob, detailJob, payload, getJobType(),
                () -> persistState(retryJob, detailJob));

        try {
            buildChain(retryJob.getNotificationEmail()).handle(context);
            logger.info("Retry job processed successfully. jobType={}, retryJobId={}", getJobType(), retryJobId);
        } catch (Exception exception) {
            handleFailure(retryJob, detailJob, payload, context.getCurrentStep(), exception);
        }
    }

    private RetryStep<T, D> buildChain(String recipient) {
        String notificationRecipient = hasText(recipient) ? recipient : properties.getNotifications().getDefaultRecipient();
        RetryOperationStep<T, D> retryOperationStep = new RetryOperationStep<>(getRetryTargetClient(),
                "Reintento de " + getEntityName() + " ejecutado correctamente");
        retryOperationStep
                .linkWith(new SuccessEmailStep<>(notificationService, notificationRecipient, buildSuccessSubject(),
                        buildSuccessMessage()))
                .linkWith(new UpdateRetryJobStep<>("Retry job de " + getEntityName() + " actualizado correctamente"));
        return retryOperationStep;
    }

    private void handleFailure(RetryJob retryJob, D detailJob, T payload, RetryStepName stepName, Exception exception) {
        RetryStepName failureStep = stepName == null ? RetryStepName.DATA : stepName;
        String errorMessage = sanitizeMessage(exception);
        retryJob.markStepFailure(failureStep, errorMessage,
                LocalDateTime.now().plusNanos(properties.getRetry().getDelayMs() * 1_000_000L));
        if (!retryJob.canAttempt(properties.getRetry().getMaxAttempts())) {
            retryJob.setLastError(errorMessage + ". Se agotaron los "
                    + properties.getRetry().getMaxAttempts() + " intentos configurados");
        }
        detailJob.markError(errorMessage);
        persistState(retryJob, detailJob);
        sendFailureNotification(retryJob, errorMessage);
        logger.error("Retry job failed. jobType={}, retryJobId={}, step={}, error={}",
                getJobType(), retryJob.getId(), failureStep, errorMessage, exception);
    }

    private void persistState(RetryJob retryJob, D detailJob) {
        retryJob.setPayload(buildPayloadSnapshot(retryJob));
        retryJobRepository.save(retryJob);
        if (shouldPersistDetailJob(detailJob)) {
            saveDetailJob(detailJob);
        }
    }

    private void sendFailureNotification(RetryJob retryJob, String errorMessage) {
        try {
            String recipient = hasText(retryJob.getNotificationEmail())
                    ? retryJob.getNotificationEmail()
                    : properties.getNotifications().getDefaultRecipient();
            notificationService.sendFailure(getJobType(), recipient, buildFailureSubject(),
                    "Fallo en el procesamiento del " + getEntityName() + " con id de referencia "
                            + fallbackValue(retryJob.getReferenceId()) + ". Error: " + errorMessage);
        } catch (Exception notificationException) {
            logger.error("Failure notification could not be sent. jobType={}, retryJobId={}, error={}",
                    getJobType(), retryJob.getId(), sanitizeMessage(notificationException), notificationException);
        }
    }

    private String buildPayloadSnapshot(RetryJob retryJob) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> data = readRequestPayloadAsMap(retryJob);
        if (data.containsKey("status") && !data.containsKey("originalStatus")) {
            data.put("originalStatus", data.get("status"));
        }
        data.put("status", retryJob.getDataStatus());
        if (retryJob.getDataMessage() != null) {
            data.put("message", retryJob.getDataMessage());
        } else {
            data.remove("message");
        }
        snapshot.put("data", data);
        snapshot.put("sendEmail", buildStepSnapshot(retryJob.getSendEmailStatus(), retryJob.getSendEmailMessage()));
        snapshot.put("updateRetryJobs",
                buildStepSnapshot(retryJob.getUpdateRetryJobsStatus(), retryJob.getUpdateRetryJobsMessage()));

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el snapshot del retry job", exception);
        }
    }

    private Map<String, Object> buildStepSnapshot(RetryExecutionStatus status, String message) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status);
        snapshot.put("message", message);
        return snapshot;
    }

    private D buildTransientDetailJob(Long retryJobId, T payload) {
        D detailJob = createDetailJob(payload);
        detailJob.setRetryJobId(retryJobId);
        if (detailJob.getExecutionStatus() == null) {
            detailJob.setExecutionStatus(RetryExecutionStatus.PENDING);
        }
        return detailJob;
    }

    private boolean shouldPersistDetailJob(D detailJob) {
        return detailJob != null;
    }

    private T readPayload(RetryJob retryJob) {
        String requestPayload = retryJob.getRequestPayload();
        if (!hasText(requestPayload)) {
            requestPayload = rebuildRequestPayloadFromSnapshot(retryJob);
            retryJob.setRequestPayload(requestPayload);
        }

        try {
            return objectMapper.readValue(requestPayload, getPayloadClass());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo reconstruir el payload del retry job", exception);
        }
    }

    private String extractRequestPayload(String rawMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawMessage);
            JsonNode dataNode = rootNode.get("data");
            if (dataNode == null || dataNode.isNull()) {
                throw new IllegalArgumentException("El mensaje del retry debe contener el nodo data");
            }
            return objectMapper.writeValueAsString(dataNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("No se pudo deserializar el payload original del retry", exception);
        }
    }

    private Map<String, Object> readRequestPayloadAsMap(RetryJob retryJob) {
        String requestPayload = retryJob.getRequestPayload();
        if (!hasText(requestPayload)) {
            requestPayload = rebuildRequestPayloadFromSnapshot(retryJob);
            retryJob.setRequestPayload(requestPayload);
        }

        try {
            return objectMapper.readValue(requestPayload, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo reconstruir el payload original del retry job", exception);
        }
    }

    private String rebuildRequestPayloadFromSnapshot(RetryJob retryJob) {
        Map<String, Object> data = extractDataSnapshot(retryJob.getPayload());
        data.remove("message");
        data.remove("operationStatus");
        if (data.containsKey("originalStatus")) {
            data.put("status", data.remove("originalStatus"));
        } else {
            data.remove("status");
        }

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo reconstruir el payload original del retry job", exception);
        }
    }

    private Map<String, Object> extractDataSnapshot(String payloadSnapshot) {
        if (!hasText(payloadSnapshot)) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> snapshot = objectMapper.readValue(payloadSnapshot,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            Object dataNode = snapshot.get("data");
            if (dataNode == null) {
                return new LinkedHashMap<>();
            }
            return objectMapper.convertValue(dataNode, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo leer el snapshot del retry job", exception);
        }
    }

    private RetryEnvelope<T> readEnvelope(String rawMessage) {
        JavaType javaType = objectMapper.getTypeFactory()
                .constructParametricType(RetryEnvelope.class, getPayloadClass());
        try {
            return objectMapper.readValue(rawMessage, javaType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("No se pudo deserializar el mensaje del topico", exception);
        }
    }

    private T requirePayload(RetryEnvelope<T> envelope) {
        if (envelope == null || envelope.getData() == null) {
            throw new IllegalArgumentException("El mensaje del retry debe contener el nodo data");
        }
        return envelope.getData();
    }

    private String resolveRecipient(RetryEnvelope<T> envelope, T payload) {
        if (envelope.getMetadata() != null && hasText(envelope.getMetadata().getEmail())) {
            return envelope.getMetadata().getEmail().trim();
        }
        if (hasText(payload.getNotificationEmail())) {
            return payload.getNotificationEmail().trim();
        }
        return properties.getNotifications().getDefaultRecipient();
    }

    private String buildJobKey(T payload, String rawMessage) {
        if (hasText(payload.getId())) {
            return getJobType() + ":" + payload.getId().trim();
        }
        return getJobType() + ":" + sha256(rawMessage);
    }

    private String sha256(String rawValue) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular el hash para el job key", exception);
        }
    }

    private String sanitizeMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Error no controlado durante el procesamiento del retry job";
        }
        return exception.getMessage();
    }

    private String fallbackValue(String value) {
        return hasText(value) ? value : "N/A";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    protected String buildSuccessSubject() {
        return capitalize(getEntityName()) + " creado correctamente";
    }

    protected String buildSuccessMessage() {
        return capitalize(getEntityName()) + " creado correctamente mediante retry job";
    }

    protected String buildFailureSubject() {
        return capitalize(getEntityName()) + " fallido";
    }

    protected String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    protected abstract Class<T> getPayloadClass();

    protected abstract String getEntityName();

    protected abstract void validatePayload(T payload);

    protected abstract D createDetailJob(T payload);

    protected abstract Optional<D> findDetailJob(Long retryJobId);

    protected abstract D saveDetailJob(D detailJob);

    protected abstract RetryTargetClient<T> getRetryTargetClient();
}
