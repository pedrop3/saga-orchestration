package com.learn.orchestrated.order.service.dto;

import com.learn.sagacommons.dto.OrderProducts;
import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {

    private List<OrderProducts> products;
}
