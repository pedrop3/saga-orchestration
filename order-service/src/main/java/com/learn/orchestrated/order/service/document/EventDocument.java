package com.learn.orchestrated.order.service.document;

import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document("event")
public class EventDocument extends Event {

    @Id
    private String eventId;


    //TODO  Review inheritance structure
    public static EventDocument fromEvent(Event event) {
        EventDocument eventDocument = new EventDocument();
        eventDocument.setEventId(event.getEventId());
        eventDocument.setTransactionId(event.getTransactionId());
        eventDocument.setOrderId(event.getOrderId());
        eventDocument.setOrder(event.getOrder());
        eventDocument.setSource(event.getSource());
        eventDocument.setStatus(event.getStatus());
        eventDocument.setHistory(event.getHistory());
        eventDocument.setCreatedAt(event.getCreatedAt());
        return eventDocument;
    }
}
