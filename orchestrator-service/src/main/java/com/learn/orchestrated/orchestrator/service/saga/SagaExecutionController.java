package com.learn.orchestrated.orchestrator.service.saga;

import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.exception.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.learn.orchestrated.orchestrator.service.saga.SagaHandler.*;
import static java.lang.String.format;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Component
@AllArgsConstructor
public class SagaExecutionController {

    private static final String SAGA_LOG_ID = "ORDER ID: %s | TRANSACTION ID %s | EVENT ID %s";

    public TopicsEnum getNextTopic(Event event) {
        if (isEmpty(event.getSource()) || isEmpty(event.getStatus())) {
            throw new ValidationException("Source and status must be informed.");
        }
        var topic = findTopicBySourceAndStatus(event);
        logCurrentSaga(event, topic);

        return topic;
    }

    private TopicsEnum findTopicBySourceAndStatus(Event event) {
        return (TopicsEnum) (Arrays.stream(SAGA_HANDLER)
                .filter(row -> isEventSourceAndStatusValid(event, row))
                .map(i -> i[TOPIC_INDEX])
                .findFirst()
                .orElseThrow(() -> new ValidationException("Topic not found!")));
    }

    private boolean isEventSourceAndStatusValid(Event event,
                                                Object[] row) {
        var source = row[EVENT_SOURCE_INDEX];
        var status = row[SAGA_STATUS_INDEX];
        return source.toString().equals(event.getSource()) && status.equals(event.getStatus());
    }

    private void logCurrentSaga(Event event, TopicsEnum topic) {
        var sagaId = createSagaId(event);
        var source = event.getSource();
        switch (event.getStatus()) {
            case SUCCESS -> log.info("### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case ROLLBACK-> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case FAIL -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
        }
    }

    private String createSagaId(Event event) {
        return format(SAGA_LOG_ID,
                event.getOrder().getOrderId(), event.getTransactionId(), event.getEventId());
    }
}