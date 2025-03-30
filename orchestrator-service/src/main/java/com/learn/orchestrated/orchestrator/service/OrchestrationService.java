package com.learn.orchestrated.orchestrator.service;

import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.producer.SagaOrchestratorProducer;
import com.learn.orchestrated.orchestrator.service.saga.SagaExecutionController;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum.ORCHESTRATOR;
import static com.learn.sagacommons.enums.SagaStatusEnum.FAIL;
import static com.learn.sagacommons.enums.SagaStatusEnum.SUCCESS;

@Slf4j
@Service
@AllArgsConstructor
public class OrchestrationService {

    private final SagaOrchestratorProducer producer;
    private final JsonUtil jsonUtil;
    private final SagaExecutionController sagaExecutionController;

    public void startSaga(Event event) {
        event.setSource(ORCHESTRATOR.toString());
        event.setStatus(SUCCESS);
        var topic = getTopic(event);
        log.info("SAGA STARTED!");
        addHistory(event, "Saga started!");
        sendToProducerWithTopic(event, topic);
    }

    public void finishSagaSuccess(Event event) {
        event.setSource(ORCHESTRATOR.toString());
        event.setStatus(SUCCESS);
        log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT {}!", event.getEventId());
        addHistory(event, "Saga finished successfully!");
        notifyFinishedSaga(event);
    }

    public void finishSagaFail(Event event) {
        event.setSource(ORCHESTRATOR.toString());
        event.setStatus(FAIL);
        log.info("SAGA FINISHED WITH ERRORS FOR EVENT {}!", event.getEventId());
        addHistory(event, "Saga finished with errors!");
        notifyFinishedSaga(event);
    }

    public void continueSaga(Event event) {
        var topic = getTopic(event);
        log.info("SAGA CONTINUING FOR EVENT {}", event.getEventId());
        sendToProducerWithTopic(event, topic);
    }

    private TopicsEnum getTopic(Event event) {
        return sagaExecutionController.getNextTopic(event);
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus().toString())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }

    private void sendToProducerWithTopic(Event event, TopicsEnum topic) {
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow(), topic.getTopic());
    }

    private void notifyFinishedSaga(Event event) {
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow(), TopicsEnum.NOTIFY_ENDING.getTopic());
    }

}