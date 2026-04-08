package com.example.broker_message_be.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.ProductRetryPayload;

@Component
public class ProductRetryTargetClient implements RetryTargetClient<ProductRetryPayload> {

    private final RestClient restClient;

    public ProductRetryTargetClient(RestClient.Builder restClientBuilder, BrokerProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getServices().getProductsUrl())
                .build();
    }

    @Override
    public void retry(ProductRetryPayload payload) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("nombre", payload.getNombre());
        request.put("precio", payload.getPrecio());

        restClient.post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
