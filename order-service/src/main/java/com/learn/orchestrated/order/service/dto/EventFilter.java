package com.learn.orchestrated.order.service.dto;

import com.learn.orchestrated.order.service.anotation.AtLeastOneNotEmpty;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AtLeastOneNotEmpty(fields = {"orderId", "transactionId"},
        message = "At least one of the 'orderId' or 'transactionId' fields must be filled.")
public record EventFilter(String orderId, String transactionId) {}
