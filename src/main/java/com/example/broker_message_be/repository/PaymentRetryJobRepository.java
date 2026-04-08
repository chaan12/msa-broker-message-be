package com.example.broker_message_be.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.broker_message_be.entity.PaymentRetryJob;

public interface PaymentRetryJobRepository extends JpaRepository<PaymentRetryJob, Long> {

    Optional<PaymentRetryJob> findByRetryJobId(Long retryJobId);
}
