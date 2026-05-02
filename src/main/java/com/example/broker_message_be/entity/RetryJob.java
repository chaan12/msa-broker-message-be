package com.example.broker_message_be.entity;

import java.time.LocalDateTime;

import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.enumtype.RetryStepName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "retry_jobs")
public class RetryJob extends TimestampedEntity {

    private static final String SEND_EMAIL_PENDING_MESSAGE = "Pendiente de ejecutar el paso de envio de correo";
    private static final String UPDATE_RETRY_JOB_PENDING_MESSAGE =
            "Pendiente de ejecutar el paso de actualizacion del retry job";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_key", nullable = false, unique = true)
    private String jobKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private RetryJobType jobType;

    @Column(name = "source_topic", nullable = false)
    private String sourceTopic;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetryExecutionStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_status", nullable = false)
    private RetryExecutionStatus dataStatus;

    @Column(name = "data_message", columnDefinition = "TEXT")
    private String dataMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_email_status", nullable = false)
    private RetryExecutionStatus sendEmailStatus;

    @Column(name = "send_email_message", columnDefinition = "TEXT")
    private String sendEmailMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_retry_jobs_status", nullable = false)
    private RetryExecutionStatus updateRetryJobsStatus;

    @Column(name = "update_retry_jobs_message", columnDefinition = "TEXT")
    private String updateRetryJobsMessage;

    public Long getId() {
        return id;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public RetryJobType getJobType() {
        return jobType;
    }

    public void setJobType(RetryJobType jobType) {
        this.jobType = jobType;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public RetryExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(RetryExecutionStatus status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public boolean canAttempt(int maxAttempts) {
        return retryCount < maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public RetryExecutionStatus getDataStatus() {
        return dataStatus;
    }

    public String getDataMessage() {
        return dataMessage;
    }

    public RetryExecutionStatus getSendEmailStatus() {
        return sendEmailStatus;
    }

    public String getSendEmailMessage() {
        return sendEmailMessage;
    }

    public RetryExecutionStatus getUpdateRetryJobsStatus() {
        return updateRetryJobsStatus;
    }

    public String getUpdateRetryJobsMessage() {
        return updateRetryJobsMessage;
    }

    public void initializeForNewJob(LocalDateTime nextRetryAt) {
        this.status = RetryExecutionStatus.PENDING;
        this.retryCount = 0;
        this.lastError = null;
        this.lastAttemptAt = null;
        this.nextRetryAt = nextRetryAt;
        this.dataStatus = RetryExecutionStatus.PENDING;
        this.dataMessage = null;
        this.sendEmailStatus = RetryExecutionStatus.PENDING;
        this.sendEmailMessage = SEND_EMAIL_PENDING_MESSAGE;
        this.updateRetryJobsStatus = RetryExecutionStatus.PENDING;
        this.updateRetryJobsMessage = UPDATE_RETRY_JOB_PENDING_MESSAGE;
    }

    public boolean isStepSuccessful(RetryStepName stepName) {
        return switch (stepName) {
            case DATA -> dataStatus == RetryExecutionStatus.SUCCESS;
            case SEND_EMAIL -> sendEmailStatus == RetryExecutionStatus.SUCCESS;
            case UPDATE_RETRY_JOBS -> updateRetryJobsStatus == RetryExecutionStatus.SUCCESS;
        };
    }

    public void markStepSuccess(RetryStepName stepName, String message) {
        switch (stepName) {
            case DATA -> {
                dataStatus = RetryExecutionStatus.SUCCESS;
                dataMessage = message;
            }
            case SEND_EMAIL -> {
                sendEmailStatus = RetryExecutionStatus.SUCCESS;
                sendEmailMessage = message;
            }
            case UPDATE_RETRY_JOBS -> {
                updateRetryJobsStatus = RetryExecutionStatus.SUCCESS;
                updateRetryJobsMessage = message;
            }
        }
        markJobSuccessIfAllStepsSuccessful();
    }

    public void markStepFailure(RetryStepName stepName, String message, LocalDateTime nextRetryTime) {
        switch (stepName) {
            case DATA -> {
                dataStatus = RetryExecutionStatus.ERROR;
                dataMessage = message;
            }
            case SEND_EMAIL -> {
                sendEmailStatus = RetryExecutionStatus.ERROR;
                sendEmailMessage = message;
            }
            case UPDATE_RETRY_JOBS -> {
                updateRetryJobsStatus = RetryExecutionStatus.ERROR;
                updateRetryJobsMessage = message;
            }
        }
        status = RetryExecutionStatus.ERROR;
        lastError = message;
        nextRetryAt = nextRetryTime;
        retryCount++;
    }

    private void markJobSuccessIfAllStepsSuccessful() {
        if (dataStatus == RetryExecutionStatus.SUCCESS
                && sendEmailStatus == RetryExecutionStatus.SUCCESS
                && updateRetryJobsStatus == RetryExecutionStatus.SUCCESS) {
            status = RetryExecutionStatus.SUCCESS;
            lastError = null;
        }
    }
}
