package com.learn.sagacommons.dto;


import com.learn.sagacommons.enums.SagaStatusEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<History> history;
    private LocalDateTime createdAt;

}
