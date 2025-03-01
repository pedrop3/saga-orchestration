package com.learn.orchestrated.order.service.dto;

import com.learn.orchestrated.order.service.anotation.AtLeastOneNotEmpty;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AtLeastOneNotEmpty(fields = {"orderId", "transactionId"},
        message = "Pelo menos um dos campos 'orderId' ou 'transactionId' deve ser preenchido.")
public record EventFilter(String orderId, String transactionId) {}
