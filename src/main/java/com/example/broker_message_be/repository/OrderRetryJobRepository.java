package com.example.broker_message_be.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.broker_message_be.entity.OrderRetryJob;

public interface OrderRetryJobRepository extends JpaRepository<OrderRetryJob, Long> {

    Optional<OrderRetryJob> findByRetryJobId(Long retryJobId);
}
