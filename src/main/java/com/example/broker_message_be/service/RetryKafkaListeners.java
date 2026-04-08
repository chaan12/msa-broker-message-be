package com.example.broker_message_be.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "broker.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class RetryKafkaListeners {

    private static final Logger logger = LoggerFactory.getLogger(RetryKafkaListeners.class);

    private final PaymentRetryJobProcessor paymentRetryJobProcessor;
    private final OrderRetryJobProcessor orderRetryJobProcessor;
    private final ProductRetryJobProcessor productRetryJobProcessor;

    public RetryKafkaListeners(PaymentRetryJobProcessor paymentRetryJobProcessor,
            OrderRetryJobProcessor orderRetryJobProcessor,
            ProductRetryJobProcessor productRetryJobProcessor) {
        this.paymentRetryJobProcessor = paymentRetryJobProcessor;
        this.orderRetryJobProcessor = orderRetryJobProcessor;
        this.productRetryJobProcessor = productRetryJobProcessor;
    }

    @KafkaListener(topics = "${broker.topics.payments}")
    public void listenPayments(ConsumerRecord<String, String> record) {
        logger.info("Payment retry message consumed. topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        paymentRetryJobProcessor.handleTopicMessage(record.value(), record.topic());
    }

    @KafkaListener(topics = "${broker.topics.orders}")
    public void listenOrders(ConsumerRecord<String, String> record) {
        logger.info("Order retry message consumed. topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        orderRetryJobProcessor.handleTopicMessage(record.value(), record.topic());
    }

    @KafkaListener(topics = "${broker.topics.products}")
    public void listenProducts(ConsumerRecord<String, String> record) {
        logger.info("Product retry message consumed. topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        productRetryJobProcessor.handleTopicMessage(record.value(), record.topic());
    }
}
