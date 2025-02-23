package com.learn.orchestrated.order.service.service.impl;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.service.EventService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    public EventDocument save(EventDocument eventDocument) {
        return eventRepository.save(eventDocument);
    }


}
