package com.learn.orchestrated.payment_service.dto;


import com.learn.orchestrated.payment_service.enums.SagaStatusEnum;
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
    private String source;
    private SagaStatusEnum status;
    private List<History> history;
}
