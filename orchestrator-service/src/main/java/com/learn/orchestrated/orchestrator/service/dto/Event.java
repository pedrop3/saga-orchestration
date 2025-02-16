package com.learn.orchestrated.orchestrator.service.dto;

import com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.enums.SagaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private String eventId;
    private String transactionId;
    private String orderId;
    private Order order;
    private EventSourceEnum source;
    private SagaStatusEnum status;
    private List<History> history;
}
