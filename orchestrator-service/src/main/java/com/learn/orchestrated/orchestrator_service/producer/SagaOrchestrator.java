package com.learn.orchestrated.orchestrator_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String playload, String topic) {
        try {
            kafkaTemplate.send(topic, playload);
            log.info("Success to send data to topic {} with data {}", topic, playload);

        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", topic, playload, e);
        }
    }
}
