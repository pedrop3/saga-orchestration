package com.learn.orchestrated.order.service.document;

import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.dto.OrderProducts;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document("order")
public class OrderDocument extends Order {

    @Id
    private String orderId;

    public OrderDocument(List<OrderProducts> products, LocalDateTime createdAt, String transactionId) {
        super();
    }
}
