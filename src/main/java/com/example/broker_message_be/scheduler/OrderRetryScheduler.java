package com.example.broker_message_be.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.broker_message_be.service.OrderRetryJobProcessor;

@Component
@ConditionalOnProperty(name = "broker.schedulers.enabled", havingValue = "true", matchIfMissing = true)
public class OrderRetryScheduler {

    private final OrderRetryJobProcessor orderRetryJobProcessor;

    public OrderRetryScheduler(OrderRetryJobProcessor orderRetryJobProcessor) {
        this.orderRetryJobProcessor = orderRetryJobProcessor;
    }

    @Scheduled(fixedDelayString = "${broker.retry.delay-ms}")
    public void processOrders() {
        orderRetryJobProcessor.processPendingJobs();
    }
}
