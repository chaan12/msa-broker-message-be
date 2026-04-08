package com.example.broker_message_be.gateway;

import com.example.broker_message_be.dto.RetryPayload;

public interface RetryTargetClient<T extends RetryPayload> {

    void retry(T payload);
}
