package com.example.broker_message_be.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.example.broker_message_be.enumtype.RetryJobType;

@Service
@ConditionalOnExpression("'${spring.mail.host:}' == ''")
public class LoggingNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void sendSuccess(RetryJobType jobType, String recipient, String subject, String message) {
        logger.info("Log notification sent. type=SUCCESS, jobType={}, recipient={}, subject={}, message={}",
                jobType, recipient, subject, message);
    }

    @Override
    public void sendFailure(RetryJobType jobType, String recipient, String subject, String message) {
        logger.warn("Log notification sent. type=FAILURE, jobType={}, recipient={}, subject={}, message={}",
                jobType, recipient, subject, message);
    }
}
