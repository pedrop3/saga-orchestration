package com.learn.sagacommons.dto;


import com.learn.sagacommons.enums.SagaStatusEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    protected String eventId;
    private String transactionId;
    private String orderId;
    private Order order;
    private String source;
    private SagaStatusEnum status;
    private List<History> eventHistory;
    private LocalDateTime createdAt;


    public void addToHistory(History history) {
        if (isEmpty(history) || eventHistory == null) {
            eventHistory = new ArrayList<>();
        }
        eventHistory.add(history);
    }

}
