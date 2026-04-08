package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;
import com.example.broker_message_be.enumtype.RetryStepName;

public class UpdateRetryJobStep<T extends RetryPayload, D extends BaseRetryDetailJob> extends AbstractRetryStep<T, D> {

    private final String successMessage;

    public UpdateRetryJobStep(String successMessage) {
        this.successMessage = successMessage;
    }

    @Override
    public void handle(RetryChainContext<T, D> context) {
        if (context.getRetryJob().isStepSuccessful(RetryStepName.UPDATE_RETRY_JOBS)) {
            handleNext(context);
            return;
        }

        context.setCurrentStep(RetryStepName.UPDATE_RETRY_JOBS);
        context.getRetryJob().markStepSuccess(RetryStepName.UPDATE_RETRY_JOBS, successMessage);
        context.getDetailJob().markSuccess();
        context.checkpoint();
        handleNext(context);
    }
}
