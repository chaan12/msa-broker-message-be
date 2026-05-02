package com.example.broker_message_be;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.broker_message_be.entity.OrderRetryJob;
import com.example.broker_message_be.entity.PaymentRetryJob;
import com.example.broker_message_be.entity.ProductRetryJob;
import com.example.broker_message_be.entity.RetryJob;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.repository.OrderRetryJobRepository;
import com.example.broker_message_be.repository.PaymentRetryJobRepository;
import com.example.broker_message_be.repository.ProductRetryJobRepository;
import com.example.broker_message_be.repository.RetryJobRepository;
import com.example.broker_message_be.service.PaymentRetryJobProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:broker_retry;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=broker-retry-flow-test",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "broker.kafka.enabled=true",
        "broker.schedulers.enabled=false",
        "spring.mail.host=test-smtp.local",
        "spring.mail.port=2525",
        "spring.mail.username=chanxortiz@gmail.com",
        "spring.mail.password=naepebyxvyecevsp",
        "broker.retry.delay-ms=1",
        "broker.retry.max-attempts=5"
})
@Import(BrokerRetryFlowIntegrationTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"payments_retry_jobs", "order_retry_jobs", "product_retry_jobs"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BrokerRetryFlowIntegrationTest {

    private static final StubRetryTargetServer RETRY_TARGET_SERVER = new StubRetryTargetServer();

    static {
        RETRY_TARGET_SERVER.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("broker.services.payments-url", () -> RETRY_TARGET_SERVER.url("/pagos/procesar"));
        registry.add("broker.services.orders-url", () -> RETRY_TARGET_SERVER.url("/ordenes"));
        registry.add("broker.services.products-url", () -> RETRY_TARGET_SERVER.url("/productos"));
        registry.add("broker.notifications.default-recipient", () -> "chanxortiz@gmail.com");
        registry.add("broker.notifications.from", () -> "chanxortiz@gmail.com");
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RetryJobRepository retryJobRepository;

    @Autowired
    private PaymentRetryJobRepository paymentRetryJobRepository;

    @Autowired
    private OrderRetryJobRepository orderRetryJobRepository;

    @Autowired
    private ProductRetryJobRepository productRetryJobRepository;

    @Autowired
    private CapturingMailSender capturingMailSender;

    @Autowired
    private PaymentRetryJobProcessor paymentRetryJobProcessor;

    @BeforeEach
    void setUp() {
        orderRetryJobRepository.deleteAll();
        paymentRetryJobRepository.deleteAll();
        productRetryJobRepository.deleteAll();
        retryJobRepository.deleteAll();
        RETRY_TARGET_SERVER.reset();
        capturingMailSender.reset();
    }

    @AfterAll
    static void tearDown() {
        RETRY_TARGET_SERVER.stop();
    }

    @Test
    void shouldConsumePaymentRetryMessageAndCompleteSuccessfulChain() throws Exception {
        sendMessage("payments_retry_jobs", """
                {
                  "data": {
                    "id": "pay-001",
                    "ordenId": "ord-001",
                    "monto": 120.50,
                    "estado": "pendiente"
                  }
                }
                """);

        RetryJob retryJob = awaitRetryJob(RetryJobType.PAYMENT, RetryExecutionStatus.SUCCESS);
        PaymentRetryJob paymentRetryJob = awaitPaymentRetryJob(retryJob.getId(), RetryExecutionStatus.SUCCESS);

        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, retryJob.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, retryJob.getDataStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, retryJob.getSendEmailStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, retryJob.getUpdateRetryJobsStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, paymentRetryJob.getExecutionStatus());
        org.junit.jupiter.api.Assertions.assertEquals("ord-001", paymentRetryJob.getOrdenId());
        org.junit.jupiter.api.Assertions.assertEquals(120.50d, paymentRetryJob.getMonto().doubleValue());
        JsonNode paymentRequest = objectMapper.readTree(RETRY_TARGET_SERVER.lastPaymentRequest());
        org.junit.jupiter.api.Assertions.assertEquals("ord-001", paymentRequest.get("ordenId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(120.50d, paymentRequest.get("monto").asDouble());

        JsonNode snapshot = objectMapper.readTree(retryJob.getPayload());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", snapshot.get("data").get("status").asText());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", snapshot.get("sendEmail").get("status").asText());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", snapshot.get("updateRetryJobs").get("status").asText());

        List<SimpleMailMessage> sentMessages = capturingMailSender.sentMessages();
        org.junit.jupiter.api.Assertions.assertEquals(1, sentMessages.size());
        org.junit.jupiter.api.Assertions.assertEquals("Pago creado correctamente", sentMessages.get(0).getSubject());
        org.junit.jupiter.api.Assertions.assertEquals("chanxortiz@gmail.com", sentMessages.get(0).getTo()[0]);
    }

    @Test
    void shouldPersistOrderRetryFailureAndSendFailureEmailWhenEndpointFails() throws Exception {
        RETRY_TARGET_SERVER.orderStatus(500);
        RETRY_TARGET_SERVER.orderResponseBody("{\"message\":\"orden fallida\"}");

        sendMessage("order_retry_jobs", """
                {
                  "data": {
                    "id": "ord-retry-001",
                    "productoId": "prod-009",
                    "usuarioId": "usr-009",
                    "status": "pendiente"
                  }
                }
                """);

        RetryJob retryJob = awaitRetryJob(RetryJobType.ORDER, RetryExecutionStatus.ERROR);
        OrderRetryJob orderRetryJob = awaitOrderRetryJob(retryJob.getId(), RetryExecutionStatus.ERROR);

        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, retryJob.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, retryJob.getDataStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.PENDING, retryJob.getSendEmailStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.PENDING, retryJob.getUpdateRetryJobsStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, orderRetryJob.getExecutionStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(retryJob.getLastError());
        org.junit.jupiter.api.Assertions.assertTrue(retryJob.getNextRetryAt().isAfter(LocalDateTime.now().minusSeconds(1)));
        org.junit.jupiter.api.Assertions.assertTrue(RETRY_TARGET_SERVER.lastOrderRequest().contains("\"productoId\":\"prod-009\""));

        List<SimpleMailMessage> sentMessages = capturingMailSender.sentMessages();
        org.junit.jupiter.api.Assertions.assertEquals(1, sentMessages.size());
        org.junit.jupiter.api.Assertions.assertEquals("Orden fallido", sentMessages.get(0).getSubject());
    }

    @Test
    void shouldMarkProductRetryJobAsErrorWhenSuccessEmailFails() throws Exception {
        capturingMailSender.failNextSends(1);

        sendMessage("product_retry_jobs", """
                {
                  "data": {
                    "id": "prod-retry-001",
                    "name": "Teclado Mecanico",
                    "description": "Teclado mecanico compacto",
                    "price": 89.99,
                    "quantity": 25,
                    "image": "https://cdn.example.com/products/teclado.jpg",
                    "category": "Electronica",
                    "subcategory": "Perifericos",
                    "brand": "KeyMax",
                    "supplier": "Distribuidora Central SA"
                  }
                }
                """);

        RetryJob retryJob = awaitRetryJob(RetryJobType.PRODUCT, RetryExecutionStatus.ERROR);
        ProductRetryJob productRetryJob = awaitProductRetryJob(retryJob.getId(), RetryExecutionStatus.ERROR);

        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, retryJob.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.SUCCESS, retryJob.getDataStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, retryJob.getSendEmailStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.PENDING, retryJob.getUpdateRetryJobsStatus());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, productRetryJob.getExecutionStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Teclado Mecanico", productRetryJob.getNombre());
        org.junit.jupiter.api.Assertions.assertEquals("Teclado mecanico compacto", productRetryJob.getDescription());
        org.junit.jupiter.api.Assertions.assertEquals(25, productRetryJob.getQuantity());
        org.junit.jupiter.api.Assertions.assertEquals("KeyMax", productRetryJob.getBrand());
        org.junit.jupiter.api.Assertions.assertEquals(2, capturingMailSender.sendAttempts());
        org.junit.jupiter.api.Assertions.assertEquals(1, capturingMailSender.sentMessages().size());
        org.junit.jupiter.api.Assertions.assertEquals("Producto fallido", capturingMailSender.sentMessages().get(0).getSubject());
        org.junit.jupiter.api.Assertions.assertTrue(RETRY_TARGET_SERVER.lastProductRequest().contains("\"nombre\":\"Teclado Mecanico\""));
        org.junit.jupiter.api.Assertions.assertTrue(RETRY_TARGET_SERVER.lastProductRequest().contains("\"precio\":89.99"));
    }

    @Test
    void shouldStopRetryingAfterFiveFailedAttempts() throws Exception {
        RETRY_TARGET_SERVER.paymentStatus(500);
        RETRY_TARGET_SERVER.paymentResponseBody("{\"message\":\"pago fallido\"}");

        sendMessage("payments_retry_jobs", """
                {
                  "data": {
                    "id": "pay-retry-max-001",
                    "ordenId": "ord-max-001",
                    "monto": 210.00,
                    "estado": "procesado"
                  }
                }
                """);

        RetryJob retryJob = awaitRetryJob(RetryJobType.PAYMENT, RetryExecutionStatus.ERROR);
        RetryJob exhaustedJob = await(Duration.ofSeconds(10), () -> {
            paymentRetryJobProcessor.processPendingJobs();
            RetryJob currentJob = retryJobRepository.findById(retryJob.getId()).orElse(null);
            if (currentJob != null && currentJob.getRetryCount() >= 5) {
                return currentJob;
            }
            return null;
        });

        int attemptsAtLimit = RETRY_TARGET_SERVER.paymentRequestCount();
        org.junit.jupiter.api.Assertions.assertEquals(5, exhaustedJob.getRetryCount());
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, exhaustedJob.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(5, attemptsAtLimit);
        org.junit.jupiter.api.Assertions.assertTrue(exhaustedJob.getLastError().contains("Se agotaron los 5 intentos"));

        RETRY_TARGET_SERVER.paymentStatus(201);
        paymentRetryJobProcessor.processPendingJobs();

        RetryJob afterLimitJob = retryJobRepository.findById(retryJob.getId())
                .orElseThrow(() -> new AssertionError("Retry job no encontrado"));
        org.junit.jupiter.api.Assertions.assertEquals(RetryExecutionStatus.ERROR, afterLimitJob.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(5, afterLimitJob.getRetryCount());
        org.junit.jupiter.api.Assertions.assertEquals(attemptsAtLimit, RETRY_TARGET_SERVER.paymentRequestCount());
    }

    private void sendMessage(String topic, String payload) {
        KafkaTemplate<String, String> kafkaTemplate = createKafkaTemplate();
        try {
            kafkaTemplate.send(topic, payload).get();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo enviar el mensaje de prueba a Kafka", exception);
        } finally {
            kafkaTemplate.destroy();
        }
    }

    private KafkaTemplate<String, String> createKafkaTemplate() {
        java.util.Map<String, Object> configs = new java.util.HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configs));
    }

    private RetryJob awaitRetryJob(RetryJobType jobType, RetryExecutionStatus expectedStatus) {
        return await(Duration.ofSeconds(10), () -> retryJobRepository.findAll().stream()
                .filter(retryJob -> retryJob.getJobType() == jobType && retryJob.getStatus() == expectedStatus)
                .findFirst()
                .orElse(null));
    }

    private PaymentRetryJob awaitPaymentRetryJob(Long retryJobId, RetryExecutionStatus expectedStatus) {
        return await(Duration.ofSeconds(10), () -> paymentRetryJobRepository.findByRetryJobId(retryJobId)
                .filter(detailJob -> detailJob.getExecutionStatus() == expectedStatus)
                .orElse(null));
    }

    private OrderRetryJob awaitOrderRetryJob(Long retryJobId, RetryExecutionStatus expectedStatus) {
        return await(Duration.ofSeconds(10), () -> orderRetryJobRepository.findByRetryJobId(retryJobId)
                .filter(detailJob -> detailJob.getExecutionStatus() == expectedStatus)
                .orElse(null));
    }

    private ProductRetryJob awaitProductRetryJob(Long retryJobId, RetryExecutionStatus expectedStatus) {
        return await(Duration.ofSeconds(10), () -> productRetryJobRepository.findByRetryJobId(retryJobId)
                .filter(detailJob -> detailJob.getExecutionStatus() == expectedStatus)
                .orElse(null));
    }

    private <T> T await(Duration timeout, Supplier<T> supplier) {
        long deadline = System.nanoTime() + timeout.toNanos();
        T value = supplier.get();
        while (value == null && System.nanoTime() < deadline) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("La espera del test fue interrumpida", exception);
            }
            value = supplier.get();
        }
        if (value == null) {
            throw new AssertionError("No se obtuvo el resultado esperado dentro del tiempo limite");
        }
        return value;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        @Primary
        CapturingMailSender javaMailSender() {
            return new CapturingMailSender();
        }
    }

    static final class CapturingMailSender implements JavaMailSender {

        private final List<SimpleMailMessage> sentMessages = new CopyOnWriteArrayList<>();
        private final AtomicInteger failuresRemaining = new AtomicInteger();
        private final AtomicInteger attempts = new AtomicInteger();

        void reset() {
            sentMessages.clear();
            failuresRemaining.set(0);
            attempts.set(0);
        }

        void failNextSends(int count) {
            failuresRemaining.set(count);
        }

        int sendAttempts() {
            return attempts.get();
        }

        List<SimpleMailMessage> sentMessages() {
            return new ArrayList<>(sentMessages);
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            attempts.incrementAndGet();
            if (failuresRemaining.getAndUpdate(value -> value > 0 ? value - 1 : value) > 0) {
                throw new MailSendException("SMTP de prueba rechazo el correo");
            }
            sentMessages.add(copyOf(simpleMessage));
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage simpleMessage : simpleMessages) {
                send(simpleMessage);
            }
        }

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException("No se usa MimeMessage en estas pruebas");
        }

        @Override
        public MimeMessage createMimeMessage(java.io.InputStream contentStream) {
            throw new UnsupportedOperationException("No se usa MimeMessage en estas pruebas");
        }

        @Override
        public void send(MimeMessage mimeMessage)
                throws MailAuthenticationException, MailSendException, MailParseException, MailPreparationException {
            throw new UnsupportedOperationException("No se usa MimeMessage en estas pruebas");
        }

        @Override
        public void send(MimeMessage... mimeMessages)
                throws MailAuthenticationException, MailSendException, MailParseException, MailPreparationException {
            throw new UnsupportedOperationException("No se usa MimeMessage en estas pruebas");
        }

        @Override
        public void send(org.springframework.mail.javamail.MimeMessagePreparator mimeMessagePreparator)
                throws MailException {
            throw new UnsupportedOperationException("No se usa MimeMessagePreparator en estas pruebas");
        }

        @Override
        public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators)
                throws MailException {
            throw new UnsupportedOperationException("No se usa MimeMessagePreparator en estas pruebas");
        }

        private SimpleMailMessage copyOf(SimpleMailMessage simpleMessage) {
            SimpleMailMessage copy = new SimpleMailMessage();
            copy.setFrom(simpleMessage.getFrom());
            copy.setTo(simpleMessage.getTo());
            copy.setSubject(simpleMessage.getSubject());
            copy.setText(simpleMessage.getText());
            return copy;
        }
    }

    static final class StubRetryTargetServer {

        private final AtomicInteger paymentStatus = new AtomicInteger(201);
        private final AtomicInteger orderStatus = new AtomicInteger(201);
        private final AtomicInteger productStatus = new AtomicInteger(201);

        private final AtomicReference<String> paymentResponseBody = new AtomicReference<>("{}");
        private final AtomicReference<String> orderResponseBody = new AtomicReference<>("{}");
        private final AtomicReference<String> productResponseBody = new AtomicReference<>("{}");

        private final AtomicReference<String> lastPaymentRequest = new AtomicReference<>();
        private final AtomicReference<String> lastOrderRequest = new AtomicReference<>();
        private final AtomicReference<String> lastProductRequest = new AtomicReference<>();
        private final AtomicInteger paymentRequestCount = new AtomicInteger();
        private final AtomicInteger orderRequestCount = new AtomicInteger();
        private final AtomicInteger productRequestCount = new AtomicInteger();

        private HttpServer httpServer;

        void start() {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo iniciar el servidor HTTP de prueba", exception);
            }
            httpServer.createContext("/pagos/procesar",
                    exchange -> handle(exchange, paymentStatus, paymentResponseBody, lastPaymentRequest,
                            paymentRequestCount));
            httpServer.createContext("/ordenes",
                    exchange -> handle(exchange, orderStatus, orderResponseBody, lastOrderRequest,
                            orderRequestCount));
            httpServer.createContext("/productos",
                    exchange -> handle(exchange, productStatus, productResponseBody, lastProductRequest,
                            productRequestCount));
            httpServer.start();
        }

        void stop() {
            if (httpServer != null) {
                httpServer.stop(0);
            }
        }

        void reset() {
            paymentStatus.set(201);
            orderStatus.set(201);
            productStatus.set(201);
            paymentResponseBody.set("{}");
            orderResponseBody.set("{}");
            productResponseBody.set("{}");
            lastPaymentRequest.set(null);
            lastOrderRequest.set(null);
            lastProductRequest.set(null);
            paymentRequestCount.set(0);
            orderRequestCount.set(0);
            productRequestCount.set(0);
        }

        void paymentStatus(int status) {
            paymentStatus.set(status);
        }

        void paymentResponseBody(String body) {
            paymentResponseBody.set(body);
        }

        void orderStatus(int status) {
            orderStatus.set(status);
        }

        void orderResponseBody(String body) {
            orderResponseBody.set(body);
        }

        String lastPaymentRequest() {
            return Objects.requireNonNull(lastPaymentRequest.get(), "No se recibio request de pago en el stub");
        }

        String lastOrderRequest() {
            return Objects.requireNonNull(lastOrderRequest.get(), "No se recibio request de orden en el stub");
        }

        String lastProductRequest() {
            return Objects.requireNonNull(lastProductRequest.get(), "No se recibio request de producto en el stub");
        }

        int paymentRequestCount() {
            return paymentRequestCount.get();
        }

        String url(String path) {
            return "http://localhost:" + httpServer.getAddress().getPort() + path;
        }

        private void handle(HttpExchange exchange, AtomicInteger status, AtomicReference<String> responseBody,
                AtomicReference<String> lastRequest, AtomicInteger requestCount) throws IOException {
            requestCount.incrementAndGet();
            lastRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }
    }
}
