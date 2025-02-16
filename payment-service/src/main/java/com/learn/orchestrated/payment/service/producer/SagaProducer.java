package com.learn.orchestrated.payment.service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestratorTopic;

    public void sendEvent(String playload) {
        try {
            kafkaTemplate.send(orchestratorTopic, playload);
            log.info("Success to send data to topic {} with data {}", orchestratorTopic, playload);

        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", orchestratorTopic, playload, e);
        }
    }
}
