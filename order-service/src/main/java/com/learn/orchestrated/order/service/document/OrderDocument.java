package com.learn.orchestrated.order.service.document;

import com.learn.sagacommons.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document("order")
public class OrderDocument extends Order {

    @Id
    private String orderId;


}
