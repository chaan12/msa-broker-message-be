package com.example.broker_message_be.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "envios")
public class Envio extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "orden_id", nullable = false, unique = true)
    private String ordenId;

    @Column(name = "usuario_id")
    private String usuarioId;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public boolean isProcessed() {
        return processed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markError(String lastError) {
        this.processed = false;
        this.lastError = lastError;
    }
}
