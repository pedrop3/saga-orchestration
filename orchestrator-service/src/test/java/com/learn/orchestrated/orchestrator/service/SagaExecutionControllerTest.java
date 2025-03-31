package com.learn.orchestrated.orchestrator.service;

import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.saga.SagaExecutionController;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum.PAYMENT_SERVICE;
import static com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum.PRODUCT_VALIDATION_SERVICE;
import static com.learn.orchestrated.orchestrator.service.enums.TopicsEnum.INVENTORY_SUCCESS;
import static com.learn.orchestrated.orchestrator.service.enums.TopicsEnum.PRODUCT_VALIDATION_FAIL;
import static com.learn.sagacommons.enums.SagaStatusEnum.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SagaExecutionControllerTest {

    private SagaExecutionController sagaExecutionController;
    private Event event;

    @BeforeEach
    void setUp() {
        sagaExecutionController = new SagaExecutionController();

        Order order = new Order();
        order.setOrderId("order-1");

        event = new Event();
        event.setEventId("evt-1");
        event.setTransactionId("tx-1");
        event.setOrder(order);
    }

    @Test
    void shouldReturnCorrectTopicWhenValidSourceAndStatus() {
        event.setSource(PAYMENT_SERVICE.toString());
        event.setStatus(SUCCESS);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(INVENTORY_SUCCESS, topic);
    }

    @Test
    void shouldReturnFailtTopicWhenValidSourceAndStatus() {
        event.setSource(PAYMENT_SERVICE.toString());
        event.setStatus(FAIL);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(PRODUCT_VALIDATION_FAIL, topic);
    }

    @Test
    void shouldThrowExceptionWhenSourceIsNull() {
        event.setSource(null);
        event.setStatus(SUCCESS);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Source and status must be informed.", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNull() {
        event.setSource(PAYMENT_SERVICE.toString());
        event.setStatus(null);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Source and status must be informed.", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoMatchingTopicFound() {
        event.setSource(PAYMENT_SERVICE.toString());

        event.setStatus(TIMEOUT);

        Exception ex = assertThrows(ValidationException.class, () -> {
            sagaExecutionController.getNextTopic(event);
        });

        assertEquals("Topic not found!", ex.getMessage());
    }

    @Test
    void shouldFormatSagaIdCorrectlyInLogs() {
        event.setSource(PRODUCT_VALIDATION_SERVICE.toString());
        event.setStatus(ROLLBACK);

        TopicsEnum topic = sagaExecutionController.getNextTopic(event);

        assertEquals(PRODUCT_VALIDATION_FAIL, topic);
    }
}