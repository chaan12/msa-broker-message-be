package com.example.broker_message_be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.broker_message_be.entity.Envio;

public interface EnvioRepository extends JpaRepository<Envio, Long> {

    Optional<Envio> findByOrdenId(String ordenId);

    List<Envio> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);
}
