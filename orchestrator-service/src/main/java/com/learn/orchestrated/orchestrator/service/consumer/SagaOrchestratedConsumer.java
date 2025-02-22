package com.learn.orchestrated.orchestrator.service.consumer;


import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class SagaOrchestratedConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.start-saga}"
    )
    public void consumeStartSagaEvent(String payload) {
        log.info("Receiving event  {} from start-saga topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from start-sagatopic", event);

    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.orchestrator}"
    )
    public void consumeOrchestratorEvent(String payload) {
        log.info("Receiving event  {} from orchestrator topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from orchestrator topic", event);

    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.finish-success}"
    )
    public void consumeFinishSuccessEvent(String payload) {
        log.info("Receiving event  {} from finish success topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from finish success topic", event);

    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.finish-fail}"
    )
    public void consumeFinishFailEvent(String payload) {
        log.info("Receiving event  {} from finish fail topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from finish fail topic", event);

    }
}
