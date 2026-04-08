package com.example.broker_message_be.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.broker_message_be.entity.RetryJob;
import com.example.broker_message_be.enumtype.RetryExecutionStatus;
import com.example.broker_message_be.enumtype.RetryJobType;

public interface RetryJobRepository extends JpaRepository<RetryJob, Long> {

    Optional<RetryJob> findByJobKey(String jobKey);

    List<RetryJob> findByJobTypeAndStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            RetryJobType jobType,
            Collection<RetryExecutionStatus> statuses,
            LocalDateTime nextRetryAt,
            Pageable pageable);
}
