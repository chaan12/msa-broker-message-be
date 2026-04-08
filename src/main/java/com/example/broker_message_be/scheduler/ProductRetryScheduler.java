package com.example.broker_message_be.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.broker_message_be.service.ProductRetryJobProcessor;

@Component
@ConditionalOnProperty(name = "broker.schedulers.enabled", havingValue = "true", matchIfMissing = true)
public class ProductRetryScheduler {

    private final ProductRetryJobProcessor productRetryJobProcessor;

    public ProductRetryScheduler(ProductRetryJobProcessor productRetryJobProcessor) {
        this.productRetryJobProcessor = productRetryJobProcessor;
    }

    @Scheduled(fixedDelayString = "${broker.retry.delay-ms}")
    public void processProducts() {
        productRetryJobProcessor.processPendingJobs();
    }
}
