package com.learn.aisagaagent.service;

import com.learn.sagacommons.dto.Order;
import org.springframework.stereotype.Component;

@Component
public class ProfileClassifier {

    private static final double HIGH_VALUE_THRESHOLD = 200.0;

    public String classify(Order order) {
        if (order == null || order.getOrderId() == null) return "default";

        String clientType    = resolveClientType(order);
        String valueSegment  = resolveValueSegment(order, clientType);

        return clientType + ":" + valueSegment;
    }



    private String resolveClientType(Order order) {
        if (order.getClientType() == null) return "new";
        return switch (order.getClientType().toLowerCase()) {
            case "vip"                        -> "vip";
            case "returning"                  -> "returning";
            default                           -> "new";
        };
    }

    private String resolveValueSegment(Order order, String clientType) {
        if ("vip".equals(clientType)) return "any";
        return order.getTotalAmount() >= HIGH_VALUE_THRESHOLD
                ? "high-value" : "low-value";
    }
}



