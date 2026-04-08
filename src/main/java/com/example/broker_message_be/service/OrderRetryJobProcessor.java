package com.example.broker_message_be.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.OrderRetryPayload;
import com.example.broker_message_be.entity.OrderRetryJob;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;
import com.example.broker_message_be.gateway.OrderRetryTargetClient;
import com.example.broker_message_be.gateway.RetryTargetClient;
import com.example.broker_message_be.repository.OrderRetryJobRepository;
import com.example.broker_message_be.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderRetryJobProcessor extends AbstractRetryJobProcessor<OrderRetryPayload, OrderRetryJob> {

    private final OrderRetryJobRepository orderRetryJobRepository;
    private final OrderRetryTargetClient orderRetryTargetClient;

    public OrderRetryJobProcessor(ObjectMapper objectMapper, RetryJobRepository retryJobRepository,
            NotificationService notificationService, BrokerProperties properties,
            OrderRetryJobRepository orderRetryJobRepository, OrderRetryTargetClient orderRetryTargetClient) {
        super(objectMapper, retryJobRepository, notificationService, properties);
        this.orderRetryJobRepository = orderRetryJobRepository;
        this.orderRetryTargetClient = orderRetryTargetClient;
    }

    @Override
    public RetryJobType getJobType() {
        return RetryJobType.ORDER;
    }

    @Override
    protected Class<OrderRetryPayload> getPayloadClass() {
        return OrderRetryPayload.class;
    }

    @Override
    protected String getEntityName() {
        return "orden";
    }

    @Override
    protected void validatePayload(OrderRetryPayload payload) {
        if (payload.getProductoId() == null || payload.getProductoId().trim().isEmpty()) {
            throw new IllegalArgumentException("El productoId de la orden es obligatorio para el retry");
        }
        if (payload.getUsuarioId() == null || payload.getUsuarioId().trim().isEmpty()) {
            throw new IllegalArgumentException("El usuarioId de la orden es obligatorio para el retry");
        }
    }

    @Override
    protected OrderRetryJob createDetailJob(OrderRetryPayload payload) {
        OrderRetryJob detailJob = new OrderRetryJob();
        detailJob.setReferenceId(payload.getId());
        detailJob.setProductoId(payload.getProductoId().trim());
        detailJob.setUsuarioId(payload.getUsuarioId().trim());
        detailJob.setOrdenStatus(payload.getStatus());
        detailJob.setExecutionStatus(RetryExecutionStatus.PENDING);
        return detailJob;
    }

    @Override
    protected Optional<OrderRetryJob> findDetailJob(Long retryJobId) {
        return orderRetryJobRepository.findByRetryJobId(retryJobId);
    }

    @Override
    protected OrderRetryJob saveDetailJob(OrderRetryJob detailJob) {
        return orderRetryJobRepository.save(detailJob);
    }

    @Override
    protected RetryTargetClient<OrderRetryPayload> getRetryTargetClient() {
        return orderRetryTargetClient;
    }
}
