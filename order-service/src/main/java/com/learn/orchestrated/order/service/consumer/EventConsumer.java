package com.learn.orchestrated.order.service.consumer;


import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.service.EventService;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventConsumer {

    private final JsonUtil jsonUtil;
    private final EventService eventService;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.notify-ending}"
    )
    public void consumeNotifyEndingEvent(String payload) {
        log.info("Receiving ending notification event  {} from notify-ending topic", payload);

        var event = (EventDocument) jsonUtil.toEvent(payload).orElseThrow();
        log.info("Received event {} from notify-ending topic", event);

        eventService.notifyEnding(event);
    }
}
