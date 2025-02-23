package com.learn.orchestrated.order.service.service;

import com.learn.orchestrated.order.service.document.EventDocument;

public interface EventService {
    EventDocument save(EventDocument eventDocument);
}
