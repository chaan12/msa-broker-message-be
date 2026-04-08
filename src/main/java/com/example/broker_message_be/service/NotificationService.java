package com.example.broker_message_be.service;

import com.example.broker_message_be.enumtype.RetryJobType;

public interface NotificationService {

    void sendSuccess(RetryJobType jobType, String recipient, String subject, String message);

    void sendFailure(RetryJobType jobType, String recipient, String subject, String message);
}
