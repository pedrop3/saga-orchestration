package com.learn.orchestrated.order.service.order.service;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.exception.SerializationException;
import com.learn.orchestrated.order.service.producer.SagaProducer;
import com.learn.orchestrated.order.service.service.EventService;
import com.learn.orchestrated.order.service.service.impl.EventPublisherServiceImpl;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock private EventService eventService;
    @Mock private SagaProducer sagaProducer;
    @Mock private JsonUtil jsonUtil;

    @InjectMocks private EventPublisherServiceImpl eventPublisherService;

    private EventDocument event;

    @BeforeEach
    void setUp() {
        event = buildEvent("evt123");
    }

    @Test
    void shouldPublishEventSuccessfully_whenSerializationSucceeds() {
        // Arrange
        String json = "{\"eventId\":\"evt123\"}";
        when(jsonUtil.toJson(event)).thenReturn(Optional.of(json));

        eventPublisherService.publish(event);

        verify(eventService).save(event);
        verify(sagaProducer).sendEvent(json);
    }

    @Test
    void shouldThrowSerializationException_whenJsonIsEmpty() {
        when(jsonUtil.toJson(event)).thenReturn(Optional.empty());

        SerializationException exception = assertThrows(SerializationException.class, () -> {
            eventPublisherService.publish(event);
        });

        assertEquals("Falha na serialização do evento.", exception.getMessage());
        verify(eventService).save(event);
        verify(sagaProducer, never()).sendEvent(anyString());
    }

    private EventDocument buildEvent(String eventId) {
        EventDocument event = new EventDocument();
        event.setEventId(eventId);
        return event;
    }
}
