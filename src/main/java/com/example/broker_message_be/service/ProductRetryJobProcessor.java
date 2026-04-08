package com.example.broker_message_be.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.ProductRetryPayload;
import com.example.broker_message_be.entity.ProductRetryJob;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.gateway.ProductRetryTargetClient;
import com.example.broker_message_be.gateway.RetryTargetClient;
import com.example.broker_message_be.repository.ProductRetryJobRepository;
import com.example.broker_message_be.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductRetryJobProcessor extends AbstractRetryJobProcessor<ProductRetryPayload, ProductRetryJob> {

    private final ProductRetryJobRepository productRetryJobRepository;
    private final ProductRetryTargetClient productRetryTargetClient;

    public ProductRetryJobProcessor(ObjectMapper objectMapper, RetryJobRepository retryJobRepository,
            NotificationService notificationService, BrokerProperties properties,
            ProductRetryJobRepository productRetryJobRepository, ProductRetryTargetClient productRetryTargetClient) {
        super(objectMapper, retryJobRepository, notificationService, properties);
        this.productRetryJobRepository = productRetryJobRepository;
        this.productRetryTargetClient = productRetryTargetClient;
    }

    @Override
    public RetryJobType getJobType() {
        return RetryJobType.PRODUCT;
    }

    @Override
    protected Class<ProductRetryPayload> getPayloadClass() {
        return ProductRetryPayload.class;
    }

    @Override
    protected String getEntityName() {
        return "producto";
    }

    @Override
    protected void validatePayload(ProductRetryPayload payload) {
        if (payload.getNombre() == null || payload.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio para el retry");
        }
        if (payload.getPrecio() == null || payload.getPrecio().signum() < 0) {
            throw new IllegalArgumentException("El precio del producto no puede ser negativo");
        }
    }

    @Override
    protected ProductRetryJob createDetailJob(ProductRetryPayload payload) {
        ProductRetryJob detailJob = new ProductRetryJob();
        detailJob.setReferenceId(payload.getId());
        detailJob.setNombre(payload.getNombre().trim());
        detailJob.setPrecio(payload.getPrecio());
        detailJob.setExecutionStatus(RetryExecutionStatus.PENDING);
        return detailJob;
    }

    @Override
    protected Optional<ProductRetryJob> findDetailJob(Long retryJobId) {
        return productRetryJobRepository.findByRetryJobId(retryJobId);
    }

    @Override
    protected ProductRetryJob saveDetailJob(ProductRetryJob detailJob) {
        return productRetryJobRepository.save(detailJob);
    }

    @Override
    protected RetryTargetClient<ProductRetryPayload> getRetryTargetClient() {
        return productRetryTargetClient;
    }
}
