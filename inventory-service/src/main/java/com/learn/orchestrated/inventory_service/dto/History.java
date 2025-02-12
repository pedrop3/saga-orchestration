package com.learn.orchestrated.inventory_service.dto;

import com.learn.orchestrated.inventory_service.enums.SagaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class History {

    private String source;
    private SagaStatusEnum status;
    private String message;
    private LocalDateTime createdAt;
}
