package com.learn.orchestrated.inventory.service.consumer;


import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class InventoryComsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.inventory-success}"
    )
    public void consumeInventorySuccessEvent(String payload) {
        log.info("Receiving event  {} from inventory payment success topic", payload);

        var event = jsonUtil.toEvent(payload);
        log.info("Received event {} from inventory  success topic", event.toString());

    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.inventory-fail}"
    )
    public void consumeInventoryFailEvent(String payload) {
        log.info("Receiving rollback event  {} from inventory payment fail topic", payload);

        var event = jsonUtil.toEvent(payload);
        log.info("Received event {} from inventory payment fail topic", event.toString());

    }
}
