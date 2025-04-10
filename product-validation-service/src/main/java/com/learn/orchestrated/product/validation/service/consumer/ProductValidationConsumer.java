package com.learn.orchestrated.product.validation.service.consumer;


import com.learn.orchestrated.product.validation.service.service.ProductValidationService;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ProductValidationConsumer {

    private final JsonUtil jsonUtil;
    private final ProductValidationService productValidationService;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.product-validation-success}"
    )
    public void consumeProductValidationSuccessEvent(String payload) {
        log.info("Receiving event  {} from product validation success topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from product validation success topic", event);
        productValidationService.validateExistingProducts(event);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.product-validation-fail}"
    )
    public void consumeProductValidationFailEvent(String payload) {
        log.info("Receiving rollback event  {} from product validation fail topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from product validation fail topic", event);
        productValidationService.rollbackEvent(event);
    }
}
