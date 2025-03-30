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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private EventDocument event;

    @BeforeEach
    void setUp() {
        event = new EventDocument();
        event.setEventId("evt123");
        event.setTransactionId("tx456");
        event.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldNotifyEndingAndSaveEvent() {
        when(eventRepository.save(any(EventDocument.class))).thenReturn(event);

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
    void shouldFindAllEventsOrderedByCreatedAtDesc() {
        when(eventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(event));

        List<EventDocument> events = eventService.findAll();

        assertEquals(1, events.size());
        assertEquals("evt123", events.get(0).getEventId());
    }

    @Test
    void shouldFindEventByOrderId() {
        EventFilter filter = new EventFilter("order-1", "tx-x");

        when(eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc("order-1"))
                .thenReturn(Optional.of(event));

        EventDocument result = eventService.findByFilters(filter);

        assertNotNull(result);
        assertEquals("evt123", result.getEventId());
    }

    @Test
    void shouldFindEventByTransactionIdWhenOrderIdIsEmpty() {
        EventFilter filter = new EventFilter("", "tx456");

        when(eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc("tx456"))
                .thenReturn(Optional.of(event));

        EventDocument result = eventService.findByFilters(filter);

        assertNotNull(result);
        assertEquals("evt123", result.getEventId());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdNotFound() {
        EventFilter filter = new EventFilter("order-1", "tx-x");

        when(eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc("order-1"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidArgumentsException.class, () -> eventService.findByFilters(filter));
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdNotFound() {
        EventFilter filter = new EventFilter("", "tx999");

        when(eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc("tx999"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidArgumentsException.class, () -> eventService.findByFilters(filter));
    }
}