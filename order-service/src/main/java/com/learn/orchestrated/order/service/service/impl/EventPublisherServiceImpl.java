package com.learn.orchestrated.order.service.service.impl;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.exception.SerializationException;
import com.learn.orchestrated.order.service.producer.SagaProducer;
import com.learn.orchestrated.order.service.service.EventPublisherService;
import com.learn.orchestrated.order.service.service.EventService;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

    private final EventService eventService;
    private final SagaProducer sagaProducer;
    private final JsonUtil jsonUtil;

    @Override
    public void publish(EventDocument eventDocument) {

        eventService.save(eventDocument);
        sagaProducer.sendEvent(serializeEvent(eventDocument));

    }
    private String serializeEvent(EventDocument eventDocument) {
        return jsonUtil.toJson(eventDocument)
                .orElseThrow(() -> new SerializationException("Falha na serialização do evento."));
    }
}
