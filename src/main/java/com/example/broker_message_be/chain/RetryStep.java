package com.example.broker_message_be.chain;

import com.example.broker_message_be.dto.RetryPayload;
import com.example.broker_message_be.entity.BaseRetryDetailJob;

public interface RetryStep<T extends RetryPayload, D extends BaseRetryDetailJob> {

    RetryStep<T, D> linkWith(RetryStep<T, D> nextStep);

    void handle(RetryChainContext<T, D> context);
}
