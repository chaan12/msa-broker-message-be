package com.example.broker_message_be.service;

import java.math.BigDecimal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.events.OrderStatusChangedEvent;
import com.example.broker_message_be.dto.events.PaymentReceivedEvent;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "broker.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventKafkaListeners {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventKafkaListeners.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final BrokerProperties properties;
    private final EnvioService envioService;

    public DomainEventKafkaListeners(ObjectMapper objectMapper, NotificationService notificationService,
            BrokerProperties properties, EnvioService envioService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.properties = properties;
        this.envioService = envioService;
    }

    @KafkaListener(topics = "${broker.topics.payment-received:payment_received_events}")
    public void listenPaymentReceived(ConsumerRecord<String, String> record) {
        PaymentReceivedEvent event = readEvent(record.value(), PaymentReceivedEvent.class,
                "No se pudo deserializar el evento de pago recibido");
        String recipient = resolveRecipient(event.getNotificationEmail());
        notificationService.sendSuccess(RetryJobType.PAYMENT, recipient, "Pago recibido",
                "Pago recibido para la orden " + fallback(event.getOrderId())
                        + " por monto " + fallback(event.getMonto()));
        if (event.isFullyPaid() || isPaid(event.getOrderStatus(), event.getSaldoRestante())) {
            envioService.createPendingIfAbsent(event.getEventId(), event.getOrderId(), event.getUsuarioId(), recipient);
        }
        logger.info("Payment received event consumed. topic={}, offset={}, eventId={}, orderId={}",
                record.topic(), record.offset(), event.getEventId(), event.getOrderId());
    }

    @KafkaListener(topics = "${broker.topics.order-status-changed:order_status_changed_events}")
    public void listenOrderStatusChanged(ConsumerRecord<String, String> record) {
        OrderStatusChangedEvent event = readEvent(record.value(), OrderStatusChangedEvent.class,
                "No se pudo deserializar el evento de estado de orden");
        String recipient = resolveRecipient(event.getNotificationEmail());
        notificationService.sendSuccess(RetryJobType.ORDER, recipient, "Estado de orden actualizado",
                "La orden " + fallback(event.getOrderId()) + " cambio a estado " + fallback(event.getStatus()));
        if ("PAGADO".equalsIgnoreCase(event.getStatus())) {
            envioService.createPendingIfAbsent(event.getEventId(), event.getOrderId(), event.getUsuarioId(), recipient);
        }
        logger.info("Order status event consumed. topic={}, offset={}, eventId={}, orderId={}, status={}",
                record.topic(), record.offset(), event.getEventId(), event.getOrderId(), event.getStatus());
    }

    private <T> T readEvent(String payload, Class<T> eventClass, String message) {
        try {
            return objectMapper.readValue(payload, eventClass);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(message, exception);
        }
    }

    private boolean isPaid(String status, BigDecimal saldoRestante) {
        return "PAGADO".equalsIgnoreCase(status)
                || (saldoRestante != null && saldoRestante.compareTo(BigDecimal.ZERO) == 0);
    }

    private String resolveRecipient(String notificationEmail) {
        return hasText(notificationEmail) ? notificationEmail.trim() : properties.getNotifications().getDefaultRecipient();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String fallback(Object value) {
        return value == null ? "N/A" : value.toString();
    }
}
