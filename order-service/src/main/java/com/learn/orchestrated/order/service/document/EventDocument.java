package com.learn.orchestrated.order.service.document;

import com.learn.sagacommons.dto.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document("event")
public class EventDocument extends Event {

    @Id
    private String eventId;

    public static EventDocument fromEvent(Event event) {
        EventDocument eventDocument = new EventDocument();
        eventDocument.setEventId(event.getEventId());
        eventDocument.setTransactionId(event.getTransactionId());
        eventDocument.setOrderId(event.getOrderId());
        eventDocument.setOrder(event.getOrder());
        eventDocument.setSource(event.getSource());
        eventDocument.setStatus(event.getStatus());
        eventDocument.setEventHistory(event.getEventHistory());
        eventDocument.setCreatedAt(event.getCreatedAt());
        return eventDocument;
    }
}
