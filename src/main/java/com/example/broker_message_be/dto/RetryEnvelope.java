package com.example.broker_message_be.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryEnvelope<T extends RetryPayload> {

    private T data;
    private RetryStepSnapshot sendEmail;
    private RetryStepSnapshot updateRetryJobs;
    private RetryMetadata metadata;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public RetryStepSnapshot getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(RetryStepSnapshot sendEmail) {
        this.sendEmail = sendEmail;
    }

    public RetryStepSnapshot getUpdateRetryJobs() {
        return updateRetryJobs;
    }

    public void setUpdateRetryJobs(RetryStepSnapshot updateRetryJobs) {
        this.updateRetryJobs = updateRetryJobs;
    }

    public RetryMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(RetryMetadata metadata) {
        this.metadata = metadata;
    }
}
