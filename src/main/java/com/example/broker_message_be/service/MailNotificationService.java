package com.example.broker_message_be.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.enumtype.RetryJobType;

@Service
@ConditionalOnExpression("'${spring.mail.host:}' != ''")
public class MailNotificationService implements NotificationService {

    private final JavaMailSender javaMailSender;
    private final BrokerProperties properties;

    public MailNotificationService(JavaMailSender javaMailSender, BrokerProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    @Override
    public void sendSuccess(RetryJobType jobType, String recipient, String subject, String message) {
        sendMail(jobType, recipient, subject, message);
    }

    @Override
    public void sendFailure(RetryJobType jobType, String recipient, String subject, String message) {
        sendMail(jobType, recipient, subject, message);
    }

    private void sendMail(RetryJobType jobType, String recipient, String subject, String message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(properties.getNotifications().getFrom());
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText("Tipo de job: " + jobType + System.lineSeparator() + System.lineSeparator() + message);
        javaMailSender.send(email);
    }
}
