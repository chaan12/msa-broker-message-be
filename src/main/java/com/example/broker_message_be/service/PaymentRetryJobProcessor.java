package com.example.broker_message_be.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.PaymentRetryPayload;
import com.example.broker_message_be.entity.PaymentRetryJob;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.gateway.PaymentRetryTargetClient;
import com.example.broker_message_be.gateway.RetryTargetClient;
import com.example.broker_message_be.repository.PaymentRetryJobRepository;
import com.example.broker_message_be.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PaymentRetryJobProcessor extends AbstractRetryJobProcessor<PaymentRetryPayload, PaymentRetryJob> {

    private final PaymentRetryJobRepository paymentRetryJobRepository;
    private final PaymentRetryTargetClient paymentRetryTargetClient;

    public PaymentRetryJobProcessor(ObjectMapper objectMapper, RetryJobRepository retryJobRepository,
            NotificationService notificationService, BrokerProperties properties,
            PaymentRetryJobRepository paymentRetryJobRepository, PaymentRetryTargetClient paymentRetryTargetClient) {
        super(objectMapper, retryJobRepository, notificationService, properties);
        this.paymentRetryJobRepository = paymentRetryJobRepository;
        this.paymentRetryTargetClient = paymentRetryTargetClient;
    }

    @Override
    public RetryJobType getJobType() {
        return RetryJobType.PAYMENT;
    }

    @Override
    protected Class<PaymentRetryPayload> getPayloadClass() {
        return PaymentRetryPayload.class;
    }

    @Override
    protected String getEntityName() {
        return "pago";
    }

    @Override
    protected void validatePayload(PaymentRetryPayload payload) {
        if (payload.getOrdenId() == null || payload.getOrdenId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ordenId del pago es obligatorio para el retry");
        }
        if (payload.getMonto() == null || payload.getMonto().signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor que cero");
        }
    }

    @Override
    protected PaymentRetryJob createDetailJob(PaymentRetryPayload payload) {
        PaymentRetryJob detailJob = new PaymentRetryJob();
        detailJob.setReferenceId(payload.getId());
        detailJob.setOrdenId(payload.getOrdenId().trim());
        detailJob.setMonto(payload.getMonto());
        detailJob.setPagoEstado(payload.getEstado());
        detailJob.setExecutionStatus(RetryExecutionStatus.PENDING);
        return detailJob;
    }

    @Override
    protected Optional<PaymentRetryJob> findDetailJob(Long retryJobId) {
        return paymentRetryJobRepository.findByRetryJobId(retryJobId);
    }

    @Override
    protected PaymentRetryJob saveDetailJob(PaymentRetryJob detailJob) {
        return paymentRetryJobRepository.save(detailJob);
    }

    @Override
    protected RetryTargetClient<PaymentRetryPayload> getRetryTargetClient() {
        return paymentRetryTargetClient;
    }
}
