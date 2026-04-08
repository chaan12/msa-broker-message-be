package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;
import com.example.broker_message_be.enumtype.RetryStepName;
import com.example.broker_message_be.service.NotificationService;

public class SuccessEmailStep<T extends RetryPayload, D extends BaseRetryDetailJob> extends AbstractRetryStep<T, D> {

    private final NotificationService notificationService;
    private final String recipient;
    private final String subject;
    private final String successMessage;

    public SuccessEmailStep(NotificationService notificationService, String recipient, String subject,
            String successMessage) {
        this.notificationService = notificationService;
        this.recipient = recipient;
        this.subject = subject;
        this.successMessage = successMessage;
    }

    @Override
    public void handle(RetryChainContext<T, D> context) {
        if (context.getRetryJob().isStepSuccessful(RetryStepName.SEND_EMAIL)) {
            handleNext(context);
            return;
        }

        context.setCurrentStep(RetryStepName.SEND_EMAIL);
        notificationService.sendSuccess(context.getJobType(), recipient, subject, successMessage);
        context.getRetryJob().markStepSuccess(RetryStepName.SEND_EMAIL, successMessage);
        context.checkpoint();
        handleNext(context);
    }
}
