package com.example.broker_message_be.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.broker_message_be.service.PaymentRetryJobProcessor;

@Component
@ConditionalOnProperty(name = "broker.schedulers.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentRetryScheduler {

    private final PaymentRetryJobProcessor paymentRetryJobProcessor;

    public PaymentRetryScheduler(PaymentRetryJobProcessor paymentRetryJobProcessor) {
        this.paymentRetryJobProcessor = paymentRetryJobProcessor;
    }

    @Scheduled(fixedDelayString = "${broker.retry.delay-ms}")
    public void processPayments() {
        paymentRetryJobProcessor.processPendingJobs();
    }
}
