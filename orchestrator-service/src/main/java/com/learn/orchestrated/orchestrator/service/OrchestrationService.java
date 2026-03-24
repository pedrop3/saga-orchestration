package com.learn.orchestrated.orchestrator.service;


import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.producer.SagaOrchestratorProducer;
import com.learn.orchestrated.orchestrator.service.saga.SagaExecutionController;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SagaPlannerService sagaPlannerService;

    public void startSaga(Event event) {
        event.setSource(ORCHESTRATOR.toString());
        event.setStatus(SUCCESS);
        var topic = getTopic(event);
        log.info("SAGA STARTED!");
        addHistory(event, "Saga started!");
        sendToProducerWithTopic(event, topic);
    }

    public void startSagaWithAI(Event event) {
        event.setSource(ORCHESTRATOR.toString());
        event.setStatus(SUCCESS);
        // Lookup Redis ~10ms | fallback automático se Redis indisponível
        String firstTopic = sagaPlannerService.getFirstTopicForOrder(event.getOrder());
        addHistory(event, "Saga started with plan: " + firstTopic);
        producer.sendEvent(firstTopic, jsonUtil.toJson(event).orElseThrow());
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
       // 1. Tenta usar plano do AI
        String aiNextTopic = sagaPlannerService.getNextTopicForOrder(
                event.getOrder(),
                event.getSource()  // step que acabou de completar
        );

        if ("FINISH_SUCCESS".equals(aiNextTopic)) {
            finishSagaSuccess(event);
            return;
        }

        if (aiNextTopic != null) {
            // Usa plano do AI
            log.info("[Orchestrator] AI plan → next topic: {}", aiNextTopic);
            producer.sendEvent(aiNextTopic, jsonUtil.toJson(event).orElseThrow());
            return;
        }

        // 2. Fallback para SAGA_HANDLER original
        log.warn("[Orchestrator] Falling back to SAGA_HANDLER for source: {}", event.getSource());
        var topic = getTopic(event);
        sendToProducerWithTopic(event, topic);


    }

    public void rollbackSaga(Event event) {
        String rollbackTopic = sagaPlannerService.getPreviousRollbackTopic(
                event.getOrder(),
                event.getSource()
        );

        if (rollbackTopic != null) {
            log.info("[Orchestrator] AI rollback → topic: {}", rollbackTopic);
            producer.sendEvent(rollbackTopic, jsonUtil.toJson(event).orElseThrow());
            return;
        }

        // Fallback para SAGA_HANDLER
        var topic = getTopic(event);
        sendToProducerWithTopic(event, topic);
    }

    public void handleFail(Event event) {
        // Se é o primeiro passo — não há mais nada para fazer rollback
        String rollbackTopic = sagaPlannerService.getPreviousRollbackTopic(
                event.getOrder(),
                event.getSource()
        );

        if (rollbackTopic == null) {
            // Chegou ao início do plano — termina com FAIL
            log.info("[Orchestrator] Rollback chain complete — finishing with FAIL");
            finishSagaFail(event);
            return;
        }

        // Ainda há passos anteriores para fazer rollback
        log.info("[Orchestrator] Propagating rollback → {}", rollbackTopic);
        producer.sendEvent(rollbackTopic, jsonUtil.toJson(event).orElseThrow());
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
        producer.sendEvent(topic.getTopic(),jsonUtil.toJson(event).orElseThrow() );
    }

    private void notifyFinishedSaga(Event event) {
        producer.sendEvent(TopicsEnum.NOTIFY_ENDING.getTopic(),jsonUtil.toJson(event).orElseThrow() );
    }


}