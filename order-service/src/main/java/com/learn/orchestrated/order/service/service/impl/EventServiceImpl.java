package com.learn.orchestrated.order.service.service.impl;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.dto.EventFilter;
import com.learn.orchestrated.order.service.exception.InvalidArgumentsException;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.service.EventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    public void notifyEnding(EventDocument eventDocument) {

        save(eventDocument);
        log.info("Order {} with saga notified!  TransactionId: {}", eventDocument.getEventId(), eventDocument.getTransactionId());
    }

    @Override
    public EventDocument save(EventDocument eventDocument) {
        return eventRepository.save(eventDocument);
    }

    @Override
    public List<EventDocument> findAll() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public EventDocument findByFilters(EventFilter eventFilter) {
        return !eventFilter.orderId().isEmpty()
                ? this.findByOrderId(eventFilter.orderId())
                : this.findByTransactionId(eventFilter.transactionId());
    }

    private EventDocument findByOrderId(String orderId) {
        return eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc(orderId).orElseThrow(() -> new InvalidArgumentsException("Order not found"));
    }

    private EventDocument findByTransactionId(String transactionId) {
        return eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId).orElseThrow(() -> new InvalidArgumentsException("Transcation not found"));
    }
}
