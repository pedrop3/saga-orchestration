package com.learn.orchestrated.order.service.dto;

import com.learn.sagacommons.dto.OrderProducts;

import java.util.List;

public record OrderRequest(List<OrderProducts> products) {}