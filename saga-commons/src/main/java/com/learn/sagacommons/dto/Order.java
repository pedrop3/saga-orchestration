package com.learn.sagacommons.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    protected String orderId;
    private List<OrderProducts> products;
    private int quantity;
    private LocalDateTime createdAt;
    private String transactionId;
    private double totalAmount;
    private int totalItems;

    private String customerId;
    private String clientType;          // novo, vip, recorrente
    private Integer clientOrderCount;   // nº pedidos históricos
    private Double clientSuccessRate;   // taxa de sucesso histórica
    private Boolean hasDigitalProducts; // skip inventory se true
}
