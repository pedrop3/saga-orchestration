package com.learn.orchestrated.order.service.service.impl;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.service.EventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    public void notifyEnding(EventDocument eventDocument) {
        eventDocument.setOrderId(eventDocument.getOrderId());
        eventDocument.setCreatedAt(LocalDateTime.now());
        save(eventDocument);

        log.info("Order {} with saga notified!  TransactionId: {}", eventDocument.getEventId(), eventDocument.getTransactionId());

    }

    @Override
    public EventDocument save(EventDocument eventDocument) {
        return eventRepository.save(eventDocument);
    }


}
