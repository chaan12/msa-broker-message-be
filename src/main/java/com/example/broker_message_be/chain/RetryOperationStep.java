package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;
import com.example.broker_message_be.enumtype.RetryStepName;
import com.example.broker_message_be.gateway.RetryTargetClient;

public class RetryOperationStep<T extends RetryPayload, D extends BaseRetryDetailJob> extends AbstractRetryStep<T, D> {

    private final RetryTargetClient<T> retryTargetClient;
    private final String successMessage;

    public RetryOperationStep(RetryTargetClient<T> retryTargetClient, String successMessage) {
        this.retryTargetClient = retryTargetClient;
        this.successMessage = successMessage;
    }

    @Override
    public void handle(RetryChainContext<T, D> context) {
        if (context.getRetryJob().isStepSuccessful(RetryStepName.DATA)) {
            handleNext(context);
            return;
        }

        context.setCurrentStep(RetryStepName.DATA);
        retryTargetClient.retry(context.getPayload());
        context.getRetryJob().markStepSuccess(RetryStepName.DATA, successMessage);
        context.checkpoint();
        handleNext(context);
    }
}
