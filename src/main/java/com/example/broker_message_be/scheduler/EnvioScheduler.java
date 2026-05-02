package com.example.broker_message_be.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.broker_message_be.service.EnvioService;

@Component
@ConditionalOnProperty(name = "broker.schedulers.enabled", havingValue = "true", matchIfMissing = true)
public class EnvioScheduler {

    private final EnvioService envioService;

    public EnvioScheduler(EnvioService envioService) {
        this.envioService = envioService;
    }

    @Scheduled(fixedDelayString = "${broker.envios.delay-ms:10000}")
    public void processPendingEnvios() {
        envioService.processPending();
    }
}
