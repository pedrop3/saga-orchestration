package com.learn.orchestrated.order.service.service;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.dto.EventFilter;

import java.util.List;

public interface EventService {
    void notifyEnding(EventDocument eventDocument);
    EventDocument save(EventDocument eventDocument);

    List<EventDocument> findAll();

    EventDocument findByFilters(EventFilter eventFilter);
}
