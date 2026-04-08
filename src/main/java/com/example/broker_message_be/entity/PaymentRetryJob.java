package com.example.broker_message_be.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments_retry_jobs")
public class PaymentRetryJob extends BaseRetryDetailJob {

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "orden_id", nullable = false)
    private String ordenId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "pago_estado")
    private String pagoEstado;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getPagoEstado() {
        return pagoEstado;
    }

    public void setPagoEstado(String pagoEstado) {
        this.pagoEstado = pagoEstado;
    }
}
