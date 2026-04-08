package com.example.broker_message_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class BrokerMessageBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerMessageBeApplication.class, args);
    }
}
