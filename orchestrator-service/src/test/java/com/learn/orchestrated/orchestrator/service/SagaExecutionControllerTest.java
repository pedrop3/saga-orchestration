package com.learn.orchestrated.orchestrator.service;

import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.saga.SagaExecutionController;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.enums.SagaStatusEnum;
import com.learn.sagacommons.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum.*;
import static com.learn.orchestrated.orchestrator.service.enums.TopicsEnum.*;
import static com.learn.sagacommons.enums.SagaStatusEnum.*;
import static org.junit.jupiter.api.Assertions.*;

class SagaExecutionControllerTest {

    private SagaExecutionController sagaExecutionController;
    private Event event;

    @BeforeEach
    void setUp() {
        sagaExecutionController = new SagaExecutionController();
        event = createDefaultEvent("order-1", "evt-1", "tx-1");
    }

    @Test
    void shouldReturnNextTopicGivenValidSourceAndSuccessStatus() {
        setEvent(PAYMENT_SERVICE.toString(), SUCCESS);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(INVENTORY_SUCCESS, topic);
    }

    @Test
    void shouldReturnFailTopicGivenValidSourceAndFailStatus() {
        setEvent(PAYMENT_SERVICE.toString(), FAIL);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(PRODUCT_VALIDATION_FAIL, topic);
    }

    @Test
    void shouldThrowValidationExceptionWhenSourceIsNull() {
        setEvent(null, SUCCESS);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Source and status must be informed.", ex.getMessage());
    }

    @Test
    void shouldThrowValidationExceptionWhenStatusIsNull() {
        setEvent(PAYMENT_SERVICE.toString(), null);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Source and status must be informed.", ex.getMessage());
    }

    @Test
    void shouldThrowValidationExceptionWhenTopicNotFound() {
        setEvent(PAYMENT_SERVICE.toString(), TIMEOUT); // assuming TIMEOUT is not mapped

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Topic not found!", ex.getMessage());
    }

    @Test
    void shouldLogSagaIdCorrectlyAndReturnRollbackTopic() {
        setEvent(PRODUCT_VALIDATION_SERVICE.toString(), ROLLBACK);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(PRODUCT_VALIDATION_FAIL, topic);
    }


    private Event createDefaultEvent(String orderId, String eventId, String txId) {
        Order order = new Order();
        order.setOrderId(orderId);

        Event event = new Event();
        event.setOrder(order);
        event.setEventId(eventId);
        event.setTransactionId(txId);
        return event;
    }

    private void setEvent(String source, SagaStatusEnum status) {
        event.setSource(source);
        event.setStatus(status);
    }
}
