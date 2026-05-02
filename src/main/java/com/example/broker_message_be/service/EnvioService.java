package com.example.broker_message_be.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.entity.Envio;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.repository.EnvioRepository;

@Service
public class EnvioService {

    private final EnvioRepository envioRepository;
    private final NotificationService notificationService;
    private final BrokerProperties properties;

    public EnvioService(EnvioRepository envioRepository, NotificationService notificationService,
            BrokerProperties properties) {
        this.envioRepository = envioRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Transactional
    public void createPendingIfAbsent(String eventId, String ordenId, String usuarioId, String notificationEmail) {
        if (!hasText(ordenId) || envioRepository.findByOrdenId(ordenId.trim()).isPresent()) {
            return;
        }
        Envio envio = new Envio();
        envio.setEventId(hasText(eventId) ? eventId.trim() : "orden-" + ordenId.trim());
        envio.setOrdenId(ordenId.trim());
        envio.setUsuarioId(hasText(usuarioId) ? usuarioId.trim() : null);
        envio.setNotificationEmail(resolveRecipient(notificationEmail));
        envioRepository.save(envio);
    }

    @Transactional
    public void processPending() {
        List<Envio> envios = envioRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 25));
        for (Envio envio : envios) {
            try {
                notificationService.sendSuccess(RetryJobType.ORDER, envio.getNotificationEmail(),
                        "Confirmacion de envio",
                        "La orden " + envio.getOrdenId() + " fue pagada completamente. Envio confirmado.");
                envio.markProcessed();
            } catch (Exception exception) {
                envio.markError(sanitizeMessage(exception));
            }
        }
    }

    private String resolveRecipient(String notificationEmail) {
        return hasText(notificationEmail) ? notificationEmail.trim() : properties.getNotifications().getDefaultRecipient();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String sanitizeMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
