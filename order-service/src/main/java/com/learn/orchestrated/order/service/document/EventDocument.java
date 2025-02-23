package com.learn.orchestrated.order.service.document;

import com.learn.sagacommons.dto.Event;
import lombok.*;
import org.springframework.data.annotation.Id;
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


    public EventDocument(String eventId, String transactionId, OrderDocument orderDocument, LocalDateTime now) {
        super();
    }
}
