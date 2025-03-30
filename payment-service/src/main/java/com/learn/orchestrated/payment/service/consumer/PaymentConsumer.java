package com.learn.orchestrated.payment.service.consumer;


import com.learn.orchestrated.payment.service.service.PaymentService;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentConsumer {

    private final JsonUtil jsonUtil;
    private final PaymentService paymentService;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.payment-success}"
    )
    public void consumePaymentSuccessEvent(String payload) {
        log.info("Receiving event  {} from product payment success topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        paymentService.realizePayment(event);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.payment-fail}"
    )
    public void consumePaymentFailEvent(String payload) {
        log.info("Receiving rollback event  {} from product payment fail topic", payload);

        var event = jsonUtil.toEvent(payload).orElseThrow();
        paymentService.realizeRefund(event);
    }
}
