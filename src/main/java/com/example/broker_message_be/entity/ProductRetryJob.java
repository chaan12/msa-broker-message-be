package com.example.broker_message_be.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_retry_jobs")
public class ProductRetryJob extends BaseRetryDetailJob {

    @Column(name = "reference_id")
    private String referenceId;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precio;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
