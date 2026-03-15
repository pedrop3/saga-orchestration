package com.learn.aisagaagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaPlan {

    private String profileKey;         // ex: "novo:alto-valor"
    private List<String> steps;        // ex: ["PRODUCT_VALIDATION","PAYMENT","INVENTORY"]
    private String reasoning;          // explicação do agente
    private LocalDateTime computedAt;

    // Converte steps para os tópicos Kafka do orchestrator
    public String firstTopic() {
        if (steps == null || steps.isEmpty()) return "product-validation-success";
        return stepToTopic(steps.get(0));
    }

    public String stepToTopic(String step) {
        return switch (step) {
            case "PRODUCT_VALIDATION" -> "product-validation-success";
            case "FRAUD_VALIDATION"   -> "fraud-validation-success";
            case "PAYMENT"            -> "payment-success";
            case "INVENTORY"          -> "inventory-success";
            default -> throw new IllegalArgumentException("Unknown step: " + step);
        };
    }

    public String rollbackTopic(String step) {
        return switch (step) {
            case "PRODUCT_VALIDATION" -> "product-validation-fail";
            case "FRAUD_VALIDATION"   -> "fraud-validation-fail";
            case "PAYMENT"            -> "payment-fail";
            case "INVENTORY"          -> "inventory-fail";
            default -> throw new IllegalArgumentException("Unknown step: " + step);
        };
    }
}
