package com.example.broker_message_be.entity;

import com.example.broker_message_be.enumtype.RetryExecutionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseRetryDetailJob extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retry_job_id", nullable = false, unique = true)
    private Long retryJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private RetryExecutionStatus executionStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public Long getRetryJobId() {
        return retryJobId;
    }

    public void setRetryJobId(Long retryJobId) {
        this.retryJobId = retryJobId;
    }

    public RetryExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(RetryExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markSuccess() {
        executionStatus = RetryExecutionStatus.SUCCESS;
        errorMessage = null;
    }

    public void markError(String message) {
        executionStatus = RetryExecutionStatus.ERROR;
        errorMessage = message;
    }
}
