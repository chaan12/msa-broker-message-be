package com.example.broker_message_be.service;

import com.example.broker_message_be.enumtype.RetryJobType;

public interface RetryJobProcessor {

    RetryJobType getJobType();

    void handleTopicMessage(String rawMessage, String topic);

    void processPendingJobs();
}
