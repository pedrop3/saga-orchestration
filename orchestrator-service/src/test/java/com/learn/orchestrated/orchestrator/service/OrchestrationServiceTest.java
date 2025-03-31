package com.learn.orchestrated.orchestrator.service;

import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.producer.SagaOrchestratorProducer;
import com.learn.orchestrated.orchestrator.service.saga.SagaExecutionController;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.Optional;
import java.util.UUID;

import static com.learn.sagacommons.enums.SagaStatusEnum.FAIL;
import static com.learn.sagacommons.enums.SagaStatusEnum.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OrchestrationServiceTest {

    @Mock private SagaOrchestratorProducer producer;
    @Mock private JsonUtil jsonUtil;
    @Mock private SagaExecutionController sagaExecutionController;

    @InjectMocks private OrchestrationService orchestrationService;

    private Event event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        event = new Event();
        event.setEventId(UUID.randomUUID().toString());
        event.setTransactionId("tx-1");
        event.setEventHistory(new java.util.ArrayList<>());
        when(jsonUtil.toJson(any())).thenReturn(Optional.of("{json}"));
    }

    @Test
    void shouldStartSagaSuccessfully() {
        when(sagaExecutionController.getNextTopic(event)).thenReturn(TopicsEnum.PRODUCT_VALIDATION_SUCCESS);

        orchestrationService.startSaga(event);

        verify(producer).sendEvent(eq("product-validation-success"), eq("{json}"));
        assertEquals("ORCHESTRATOR", event.getSource());
        assertEquals(SUCCESS, event.getStatus());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("Saga started")));
    }

    @Test
    void shouldFinishSagaSuccessfully() {
        orchestrationService.finishSagaSuccess(event);

        verify(producer).sendEvent(eq("notify-ending"), eq("{json}"));
        assertEquals("ORCHESTRATOR", event.getSource());
        assertEquals(SUCCESS, event.getStatus());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("finished successfully")));
    }

    @Test
    void shouldFinishSagaWithFailure() {
        orchestrationService.finishSagaFail(event);

        verify(producer).sendEvent(eq("notify-ending"), eq("{json}"));
        assertEquals("ORCHESTRATOR", event.getSource());
        assertEquals(FAIL, event.getStatus());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("with errors")));
    }

    @Test
    void shouldContinueSagaSuccessfully() {
        when(sagaExecutionController.getNextTopic(event)).thenReturn(TopicsEnum.INVENTORY_SUCCESS);

        orchestrationService.continueSaga(event);

        verify(producer).sendEvent(eq("inventory-success"), eq("{json}"));
    }
}
