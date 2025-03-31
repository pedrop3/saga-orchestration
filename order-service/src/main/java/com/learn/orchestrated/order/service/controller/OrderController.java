package com.learn.orchestrated.order.service.controller;

import com.learn.orchestrated.order.service.dto.OrderRequest;
import com.learn.orchestrated.order.service.service.OrderService;
import com.learn.sagacommons.dto.Order;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/orders")

public class OrderController {

    private final OrderService orderService;

    @PostMapping
    
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        log.info("Receiving request to create new order: {}", orderRequest);
        Order createdOrder = orderService.createOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }


}
