package com.learn.orchestrated.orchestrator.service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestratorProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String topic, String payload) {
        try {
            kafkaTemplate.send(topic, payload);
            log.info("Success to send data to topic {} with data {}", topic, payload);

        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", topic, payload, e);
        }
    }
}
