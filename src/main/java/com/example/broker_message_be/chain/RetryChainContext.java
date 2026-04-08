package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;
import com.example.broker_message_be.entity.RetryJob;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.enumtype.RetryStepName;

public class RetryChainContext<T extends RetryPayload, D extends BaseRetryDetailJob> {

    private final RetryJob retryJob;
    private final D detailJob;
    private final T payload;
    private final RetryJobType jobType;
    private final Runnable checkpoint;
    private RetryStepName currentStep;

    public RetryChainContext(RetryJob retryJob, D detailJob, T payload, RetryJobType jobType, Runnable checkpoint) {
        this.retryJob = retryJob;
        this.detailJob = detailJob;
        this.payload = payload;
        this.jobType = jobType;
        this.checkpoint = checkpoint;
    }

    public RetryJob getRetryJob() {
        return retryJob;
    }

    public D getDetailJob() {
        return detailJob;
    }

    public T getPayload() {
        return payload;
    }

    public RetryJobType getJobType() {
        return jobType;
    }

    public RetryStepName getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(RetryStepName currentStep) {
        this.currentStep = currentStep;
    }

    public void checkpoint() {
        checkpoint.run();
    }
}
