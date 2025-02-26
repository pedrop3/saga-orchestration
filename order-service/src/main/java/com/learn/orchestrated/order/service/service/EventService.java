package com.learn.orchestrated.order.service.service;

import com.learn.orchestrated.order.service.document.EventDocument;

public interface EventService {
    void notifyEnding(EventDocument eventDocument);
    EventDocument save(EventDocument eventDocument);
}
