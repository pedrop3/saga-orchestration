package com.learn.orchestrated.payment.service.service;

import com.learn.orchestrated.payment.service.enums.PaymentStatus;
import com.learn.orchestrated.payment.service.model.Payment;
import com.learn.orchestrated.payment.service.producer.SagaProducer;
import com.learn.orchestrated.payment.service.respository.PaymentRepository;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.learn.sagacommons.enums.SagaStatusEnum.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_VALUE_AMOUNT = 15.0;
    private static final boolean SIMULATE_RANDOM_HOUR = true; // toggle fácil
    // Mock Payment
    private static final Double MAX_AMOUNT_PER_ORDER   = 500.0;  // limite por pedido
    private static final Double MAX_AMOUNT_NEW_CUSTOMER = 500.0;  // limite cliente novo
    private static final int    FRAUD_SCORE_THRESHOLD  = 75;      // score de fraude
    private static final double PAYMENT_FAILURE_RATE   = 0.15;    // 15% falha aleatória (gateway)

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final PaymentRepository paymentRepository;


    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);

            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            validateBusinessRules(event, payment);   // ← novo
            simulateGatewayProcessing(event, payment); // ← novo
            changePaymentToSuccess(payment);

            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to make payment: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    // ─── Rules ───────────────────────────────────────────

    private void checkCurrentValidation(Event event) {
        if (Boolean.TRUE.equals(paymentRepository.existsByOrderIdAndTransactionId(
                event.getOrder().getOrderId(), event.getTransactionId()))
        ) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }
    private void validateBusinessRules(Event event, Payment payment) {
        validateOrderLimit(payment.getTotalAmount());
        validateCustomerLimit(event, payment.getTotalAmount());
        validateFraudScore(event);
        validateBlacklist(event);
    }

    private void validateOrderLimit(double amount) {
        if (amount > MAX_AMOUNT_PER_ORDER) {
            throw new ValidationException(
                    "Order amount R$%.2f exceeds maximum allowed R$%.2f per order."
                            .formatted(amount, MAX_AMOUNT_PER_ORDER)
            );
        }
    }

    private void validateCustomerLimit(Event event, double amount) {
        String clientType = getClientType(event);
        if ("new".equals(clientType) && amount > MAX_AMOUNT_NEW_CUSTOMER) {
            throw new ValidationException(
                    "New customer limit exceeded: R$%.2f > R$%.2f. Credit limit not yet established."
                            .formatted(amount, MAX_AMOUNT_NEW_CUSTOMER)
            );
        }
    }

    private void validateFraudScore(Event event) {
        int score = calculateFraudScore(event);
        if (score >= FRAUD_SCORE_THRESHOLD) {
            throw new ValidationException(
                    "Transaction blocked by fraud prevention. Risk score: %d/100. Manual review required."
                            .formatted(score)
            );
        }
    }

    private void validateBlacklist(Event event) {
        String customerId = getCustomerId(event);
        if (isCustomerBlacklisted(customerId)) {
            throw new ValidationException(
                    "Customer %s is temporarily suspended. Contact support."
                            .formatted(customerId)
            );
        }
    }

    // ─── Simulação de Gateway ────────────────────────────────────────

    private void simulateGatewayProcessing(Event event, Payment payment) {
        GatewayResult result = callPaymentGateway(payment.getTotalAmount());

        switch (result) {
            case INSUFFICIENT_FUNDS ->
                    throw new ValidationException(
                            "Payment declined: insufficient funds. Amount: R$%.2f"
                                    .formatted(payment.getTotalAmount())
                    );
            case CARD_EXPIRED ->
                    throw new ValidationException(
                            "Payment declined: card expired or invalid credentials."
                    );
            case GATEWAY_TIMEOUT ->
                    throw new ValidationException(
                            "Payment gateway timeout after 30s. Please retry."
                    );
            case DUPLICATE_TRANSACTION ->
                    throw new ValidationException(
                            "Duplicate transaction detected by gateway. TransactionId already processed."
                    );
            case SUCCESS -> log.info("Gateway approved payment for order: {}",
                    event.getOrder().getOrderId());
        }
    }

    private GatewayResult callPaymentGateway(double amount) {
        double random = Math.random();

        // 80% sucess
        if (random < 0.80) return GatewayResult.SUCCESS;

        // 15% falhas distribuídas realisticamente
        if (random < 0.95) return GatewayResult.INSUFFICIENT_FUNDS; // 5%
        if (random < 0.93) return GatewayResult.CARD_EXPIRED;       // 3%
        if (random < 0.96) return GatewayResult.GATEWAY_TIMEOUT;    // 3%
        return GatewayResult.DUPLICATE_TRANSACTION;                  // 4%
    }

    // ─── Fraud Score ─────────────────────────────────────────────────

    private int calculateFraudScore(Event event) {
        int score = 0;
        double amount = calculateAmount(event);
        String clientType = getClientType(event);
        LocalDateTime orderTime = getOrderDateTime(event);
        int hour = orderTime.getHour();

        // Valor alto
        if (amount > 1000) score += 20;
        if (amount > 800)  score += 10;

        // Cliente novo com valor alto
        if ("new".equals(clientType) && amount > 300) score += 25;

        // Horário suspeito (madrugada)
        if (hour >= 1 && hour <= 5) score += 20;

        // Muitos itens
        int items = calculateTotalItems(event);
        if (items > 10) score += 15;

        // Pequena variação aleatória (simula dados externos)
        score += (int)(Math.random() * 10);

        log.info("Fraud score for order {}: {}",
                event.getOrder().getOrderId(), score);
        return score;
    }


    private LocalDateTime getOrderDateTime(Event event) {
        if (!SIMULATE_RANDOM_HOUR) {
            return LocalDateTime.now();
        }
        return simulateRealisticOrderTime();
    }

    private LocalDateTime simulateRealisticOrderTime() {
        double random = Math.random();

        // Distribuição realista de pedidos ao longo do dia
        int hour;
        if      (random < 0.03) hour = randomHour(1, 4);   //  3% madrugada    01h-04h
        else if (random < 0.06) hour = randomHour(4, 7);   //  3% amanhecer    04h-07h
        else if (random < 0.16) hour = randomHour(7, 10);  // 10% manhã cedo   07h-10h
        else if (random < 0.36) hour = randomHour(10, 13); // 20% manhã        10h-13h
        else if (random < 0.56) hour = randomHour(13, 17); // 20% tarde        13h-17h
        else if (random < 0.76) hour = randomHour(17, 20); // 20% fim de tarde 17h-20h
        else if (random < 0.91) hour = randomHour(20, 23); // 15% noite        20h-23h
        else                    hour = randomHour(23, 25);  //  9% meia-noite   23h-01h

        int minute = (int)(Math.random() * 60);
        int second = (int)(Math.random() * 60);

        LocalDateTime simulated = LocalDateTime.now()
                .withHour(hour % 24)
                .withMinute(minute)
                .withSecond(second);

        log.info("[PaymentService] Simulated order time: {}h{}m (real: {}h)",
                hour % 24, minute, LocalDateTime.now().getHour());

        return simulated;
    }

    private int randomHour(int from, int to) {
        return from + (int)(Math.random() * (to - from));
    }

    private String getClientType(Event event) {
        return Optional.ofNullable(event.getOrder().getClientType())
                .orElse("default");
    }

    private String getCustomerId(Event event) {
        return Optional.ofNullable(event.getOrder().getCustomerId())
                .orElse("");
    }

    private boolean isCustomerBlacklisted(String customerId) {
        //  2% dos customers bloqueados
        return customerId != null &&
                !customerId.isBlank() &&
                Math.abs(customerId.hashCode() % 50) == 0;
    }


    private enum GatewayResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        CARD_EXPIRED,
        GATEWAY_TIMEOUT,
        DUPLICATE_TRANSACTION
    }

    private void createPendingPayment(Event event) {
        var totalAmount = calculateAmount(event);
        var totalItems = calculateTotalItems(event);
        var payment = Payment
                .builder()
                .orderId(event.getOrder().getOrderId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private double calculateAmount(Event event) {
        return event
                .getOrder()
                .getProducts()
                .stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int calculateTotalItems(Event event) {
        return event
                .getOrder()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getOrder().setTotalAmount(payment.getTotalAmount());
        event.getOrder().setTotalItems(payment.getTotalItems());
    }

    private void validateAmount(double amount) {
        if (amount < MIN_VALUE_AMOUNT) {
            throw new ValidationException("The minimal amount available is ".concat(String.valueOf(MIN_VALUE_AMOUNT)));
        }
    }

    private void changePaymentToSuccess(Payment payment) {
        log.info("Change payment to success: {}", payment);
        payment.setStatus(PaymentStatus.SUCCESS);
        save(payment);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Payment realized successfully!");
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus().toString())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to realize payment: ".concat(message));
    }

    public void realizeRefund(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            changePaymentStatusToRefund(event);
            addHistory(event, "Rollback executed for payment!");
        } catch (Exception ex) {
            addHistory(event, "Rollback not executed for payment: ".concat(ex.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    public Optional<Payment> findByOrderIdAndTransactionId(String orderId, String transactionId) {
        return paymentRepository.findByOrderIdAndTransactionId(orderId, transactionId);
    }

    public long countByStatus(PaymentStatus paymentStatus) {
        return paymentRepository.countByStatus(paymentStatus);
    }

    public long count() {
        return paymentRepository.count();
    }

    public List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(paymentStatus);
    }

    private void changePaymentStatusToRefund(Event event) {
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(PaymentStatus.REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

    private Payment findByOrderIdAndTransactionId(Event event) {
        return paymentRepository
                .findByOrderIdAndTransactionId(event.getOrder().getOrderId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by orderID and transactionID"));
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
}