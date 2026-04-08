package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;

public abstract class AbstractRetryStep<T extends RetryPayload, D extends BaseRetryDetailJob> implements RetryStep<T, D> {

    private RetryStep<T, D> nextStep;

    @Override
    public RetryStep<T, D> linkWith(RetryStep<T, D> nextStep) {
        this.nextStep = nextStep;
        return nextStep;
    }

    protected void handleNext(RetryChainContext<T, D> context) {
        if (nextStep != null) {
            nextStep.handle(context);
        }
    }
}
