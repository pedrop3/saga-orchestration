package com.learn.orchestrated.payment.service.consumer;

import com.learn.orchestrated.payment.service.service.FraudValidationService;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class FraudValidationConsumer {

    private final FraudValidationService fraudValidationService;
    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "fraud-validation-group",
            topics   = "${spring.kafka.topic.fraud-success}"
    )
    public void consumeSuccessEvent(String payload) {
        log.info("[FraudValidation] Received event: {}", payload);
        var event = jsonUtil.toEvent(payload).orElseThrow();
        fraudValidationService.validateFraud(event);
    }

    @KafkaListener(
            groupId = "fraud-validation-group",
            topics   = "${spring.kafka.topic.fraud-fail}"
    )
    public void consumeFailEvent(String payload) {
        log.info("[FraudValidation] Received rollback event: {}", payload);
        var event = jsonUtil.toEvent(payload).orElseThrow();
        fraudValidationService.rollbackFraud(event);
    }
}