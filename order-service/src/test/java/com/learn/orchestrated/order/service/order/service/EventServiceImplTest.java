package com.learn.orchestrated.order.service.order.service;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.dto.EventFilter;
import com.learn.orchestrated.order.service.exception.InvalidArgumentsException;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;

    @InjectMocks private EventServiceImpl eventService;

    private EventDocument event;

    @BeforeEach
    void setup() {
        event = buildEvent("evt123", "tx456");
    }

    @Test
    void shouldNotifyEndingAndSaveEvent() {
        when(eventRepository.save(event)).thenReturn(event);

        eventService.notifyEnding(event);

        verify(eventRepository).save(event);
    }

    @Test
    void shouldSaveEvent() {
        when(eventRepository.save(event)).thenReturn(event);

        EventDocument result = eventService.save(event);

        assertNotNull(result);
        assertEquals("evt123", result.getEventId());
        verify(eventRepository).save(event);
    }

    @Test
    void shouldReturnAllEventsOrderedByCreatedAtDesc() {
        when(eventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(event));

        List<EventDocument> result = eventService.findAll();

        assertEquals(1, result.size());
        assertEquals("evt123", result.getFirst().getEventId());
        verify(eventRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldFindEventByOrderId_whenOrderIdIsPresent() {
        EventFilter filter = buildFilter("order-1", "tx-x");
        when(eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc("order-1")).thenReturn(Optional.of(event));

        EventDocument result = eventService.findByFilters(filter);

        assertNotNull(result);
        assertEquals("evt123", result.getEventId());
        verify(eventRepository).findTop1ByOrderIdOrderByCreatedAtDesc("order-1");
    }

    @Test
    void shouldFindEventByTransactionId_whenOrderIdIsEmpty() {
        EventFilter filter = buildFilter("", "tx456");
        when(eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc("tx456")).thenReturn(Optional.of(event));

        EventDocument result = eventService.findByFilters(filter);

        assertNotNull(result);
        assertEquals("evt123", result.getEventId());
        verify(eventRepository).findTop1ByTransactionIdOrderByCreatedAtDesc("tx456");
    }

    @Test
    void shouldThrowException_whenEventByOrderIdNotFound() {
        EventFilter filter = buildFilter("order-1", "tx-x");
        when(eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc("order-1")).thenReturn(Optional.empty());

        assertThrows(InvalidArgumentsException.class, () -> eventService.findByFilters(filter));
    }

    @Test
    void shouldThrowException_whenEventByTransactionIdNotFound() {
        EventFilter filter = buildFilter("", "tx999");
        when(eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc("tx999")).thenReturn(Optional.empty());

        assertThrows(InvalidArgumentsException.class, () -> eventService.findByFilters(filter));
    }

    // ========== Helpers ==========

    private EventDocument buildEvent(String eventId, String txId) {
        EventDocument event = new EventDocument();
        event.setEventId(eventId);
        event.setTransactionId(txId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private EventFilter buildFilter(String orderId, String txId) {
        return new EventFilter(orderId, txId);
    }
}
