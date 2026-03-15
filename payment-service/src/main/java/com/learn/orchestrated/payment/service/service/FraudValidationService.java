package com.learn.orchestrated.payment.service.service;

import com.learn.orchestrated.payment.service.producer.SagaProducer;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.learn.sagacommons.enums.SagaStatusEnum.*;

@Slf4j
@Service
@AllArgsConstructor
public class FraudValidationService {

    private static final String CURRENT_SOURCE = "FRAUD_VALIDATION_SERVICE";
    private static final int BLOCK_THRESHOLD = 75;
    private static final int REVIEW_THRESHOLD = 50;

    private final SagaProducer producer;
    private final JsonUtil jsonUtil;


    public String calculateFraudScore(double amount,
                                      String clientType,
                                      int hour,
                                      int orderCount,
                                      double successRate,
                                      int totalItems) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // Espelha exactamente a lógica interna do validateFraudScore
        if (amount > 1200) { score += 25; reasons.add("very high amount R$%.2f".formatted(amount)); }
        else if (amount > 800) { score += 15; reasons.add("high amount R$%.2f".formatted(amount)); }
        else if (amount > 500) { score +=  5; reasons.add("above average R$%.2f".formatted(amount)); }

        switch (clientType) {
            case "new" -> {
                if (amount > 300) { score += 30; reasons.add("new customer high value"); }
                else              { score += 10; }
            }
            case "vip"       -> score -= 10;
            case "returning" -> {}
            default          -> score += 5;
        }

        if      (hour >= 1 && hour <= 4)  { score += 35; reasons.add("deep night (%dh)".formatted(hour)); }
        else if (hour >= 0 && hour <= 6)  { score += 20; reasons.add("early morning (%dh)".formatted(hour)); }
        else if (hour >= 23)              { score += 15; reasons.add("late night (%dh)".formatted(hour)); }

        if      (totalItems > 15) { score += 20; reasons.add("unusual quantity: " + totalItems); }
        else if (totalItems > 10) { score += 10; reasons.add("high quantity: " + totalItems); }

        if (orderCount > 3 && successRate < 70.0) {
            score += 20;
            reasons.add("poor history: %.0f%% success".formatted(successRate));
        }

        score = Math.min(score, 100);

        String level = score >= 75 ? "BLOCKED"
                : score >= 50 ? "REVIEW"
                : "APPROVED";

        log.info("[FraudScore MCP] score={} | level={} | amount={} | client={} | hour={}",
                score, level, amount, clientType, hour);

        return ("fraudScore=%d | level=%s | reasons=[%s] | " +
                "amount=%.2f | clientType=%s | hour=%d")
                .formatted(
                        score, level,
                        String.join(", ", reasons),
                        amount, clientType, hour
                );
    }

    public void validateFraud(Event event) {
        try {
            FraudAnalysis analysis = analyse(event);
            log.info("[FraudValidation] OrderId: {} | Score: {}/100 | Level: {}",
                    event.getOrder().getOrderId(),
                    analysis.score(),
                    analysis.level()
            );

            if (analysis.level() == RiskLevel.BLOCKED) {
                throw new ValidationException(
                        "Transaction blocked by fraud prevention. " +
                                "Risk score: %d/100. Reason: %s"
                                        .formatted(analysis.score(), analysis.reason())
                );
            }

            if (analysis.level() == RiskLevel.REVIEW) {
                // Aprovado mas com flag — continua a saga
                log.warn("[FraudValidation] Order {} flagged for review (score: {})",
                        event.getOrder().getOrderId(), analysis.score());
            }

            handleSuccess(event, analysis);

        } catch (Exception ex) {
            log.error("[FraudValidation] Blocked: {}", ex.getMessage());
            handleFailure(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    public void rollbackFraud(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed for fraud validation!");
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    // ─── Análise de Fraude ────────────────────────────────────────────

    private FraudAnalysis analyse(Event event) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        score += scoreByAmount(event, reasons);
        score += scoreByProfile(event, reasons);
        score += scoreByHour(reasons);
        score += scoreByQuantity(event, reasons);
        score += scoreByVelocity(event, reasons);
        score += scoreRandom();

        score = Math.min(score, 100);

        RiskLevel level = score >= BLOCK_THRESHOLD ? RiskLevel.BLOCKED
                : score >= REVIEW_THRESHOLD ? RiskLevel.REVIEW
                : RiskLevel.APPROVED;

        return new FraudAnalysis(score, level, String.join(", ", reasons));
    }

    private int scoreByAmount(Event event, List<String> reasons) {
        double amount = calculateAmount(event);
        if (amount > 1200) {
            reasons.add("very high amount R$" + amount);
            return 25;
        }
        if (amount > 800) {
            reasons.add("high amount R$" + amount);
            return 15;
        }
        if (amount > 500) {
            reasons.add("above average R$" + amount);
            return 5;
        }
        return 0;
    }

    private int scoreByProfile(Event event, List<String> reasons) {
        String profile = Optional.ofNullable(event.getOrder().getClientType())
                .orElse("default");
        double amount = calculateAmount(event);

        return switch (profile) {
            case "new" -> {
                if (amount > 300) {
                    reasons.add("new customer high value");
                    yield 30;
                }
                yield 10;
            }
            case "returning" -> 0;
            case "vip" -> -10; // VIP tem score reduzido
            default -> 5;
        };
    }

    private int scoreByHour(List<String> reasons) {
        int hour = simulateOrderHour();
        if (hour >= 1 && hour <= 4) {
            reasons.add("deep night order (%dh)".formatted(hour));
            return 35;
        }
        if (hour >= 0 && hour <= 6) {
            reasons.add("early morning order (%dh)".formatted(hour));
            return 20;
        }
        if (hour >= 23) {
            reasons.add("late night order (%dh)".formatted(hour));
            return 15;
        }
        return 0;
    }

    private int scoreByQuantity(Event event, List<String> reasons) {
        int items = event.getOrder().getProducts().stream()
                .mapToInt(OrderProducts::getQuantity)
                .sum();
        if (items > 15) {
            reasons.add("unusual quantity: " + items + " items");
            return 20;
        }
        if (items > 10) {
            reasons.add("high quantity: " + items + " items");
            return 10;
        }
        return 0;
    }

    private int scoreByVelocity(Event event, List<String> reasons) {
        // Simula verificação de velocidade (muitos pedidos do mesmo customer)
        double velocityRisk = Math.random();
        if (velocityRisk > 0.95) {
            reasons.add("unusual purchase velocity");
            return 25;
        }
        return 0;
    }

    private int scoreRandom() {
        return (int) (Math.random() * 8); // variação de dados externos
    }

    private int simulateOrderHour() {
        double r = Math.random();
        if (r < 0.03) return randomInt(1, 4);
        else if (r < 0.06) return randomInt(4, 7);
        else if (r < 0.15) return randomInt(23, 24);
        else return randomInt(8, 22);
    }

    private int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    private double calculateAmount(Event event) {
        return event.getOrder().getProducts().stream()
                .mapToDouble(p -> p.getQuantity() * p.getProduct().getUnitValue())
                .sum();
    }

    private void handleSuccess(Event event, FraudAnalysis analysis) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event,
                "Fraud validation passed! Score: %d/100. Level: %s."
                        .formatted(analysis.score(), analysis.level())
        );
    }

    private void handleFailure(Event event, String message) {
        event.setStatus(ROLLBACK);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validate fraud: ".concat(message));
    }

    private void addHistory(Event event, String message) {
        event.addToHistory(History.builder()
                .source(event.getSource())
                .status(event.getStatus().toString())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // records internos
    record FraudAnalysis(int score, RiskLevel level, String reason) {
    }

    enum RiskLevel {APPROVED, REVIEW, BLOCKED}
}