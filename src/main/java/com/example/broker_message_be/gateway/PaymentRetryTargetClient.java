package com.example.broker_message_be.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.broker_message_be.config.BrokerProperties;
import com.example.broker_message_be.dto.PaymentRetryPayload;

@Component
public class PaymentRetryTargetClient implements RetryTargetClient<PaymentRetryPayload> {

    private static final String RETRY_REQUEST_HEADER = "X-Broker-Retry";
    private final RestClient restClient;

    public PaymentRetryTargetClient(RestClient.Builder restClientBuilder, BrokerProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getServices().getPaymentsUrl())
                .build();
    }

    @Override
    public void retry(PaymentRetryPayload payload) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("ordenId", payload.getOrdenId());
        request.put("monto", payload.getMonto());
        request.put("estado", payload.getEstado());

        restClient.post()
                .uri("")
                .header(RETRY_REQUEST_HEADER, "true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
