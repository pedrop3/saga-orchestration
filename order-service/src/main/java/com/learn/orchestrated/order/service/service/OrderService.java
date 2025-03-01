package com.learn.orchestrated.order.service.service;

import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequest;

public interface OrderService {
    OrderDocument createOrder(OrderRequest orderRequest);
}
