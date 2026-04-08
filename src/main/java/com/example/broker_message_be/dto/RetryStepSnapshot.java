package com.example.broker_message_be.dto;

import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryStepSnapshot {

    private RetryExecutionStatus status;
    private String message;

    public RetryExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(RetryExecutionStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
