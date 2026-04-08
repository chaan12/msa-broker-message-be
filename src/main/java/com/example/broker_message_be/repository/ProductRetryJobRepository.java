package com.example.broker_message_be.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.broker_message_be.entity.ProductRetryJob;

public interface ProductRetryJobRepository extends JpaRepository<ProductRetryJob, Long> {

    Optional<ProductRetryJob> findByRetryJobId(Long retryJobId);
}
