package com.example.broker_message_be.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.OrderRetryPayload;

@Component
public class OrderRetryTargetClient implements RetryTargetClient<OrderRetryPayload> {

    private final RestClient restClient;

    public OrderRetryTargetClient(RestClient.Builder restClientBuilder, BrokerProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getServices().getOrdersUrl())
                .build();
    }

    @Override
    public void retry(OrderRetryPayload payload) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("productoId", payload.getProductoId());
        request.put("usuarioId", payload.getUsuarioId());
        request.put("status", payload.getStatus());

        restClient.post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
