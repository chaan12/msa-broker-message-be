package com.example.broker_message_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_retry_jobs")
public class OrderRetryJob extends BaseRetryDetailJob {

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "producto_id", nullable = false)
    private String productoId;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "orden_status")
    private String ordenStatus;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getOrdenStatus() {
        return ordenStatus;
    }

    public void setOrdenStatus(String ordenStatus) {
        this.ordenStatus = ordenStatus;
    }
}
