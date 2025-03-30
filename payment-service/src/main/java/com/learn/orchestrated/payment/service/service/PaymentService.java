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

import static com.learn.sagacommons.enums.SagaStatusEnum.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_VALUE_AMOUNT = 0.1;

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final PaymentRepository paymentRepository;

    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);

            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);

            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to make payment: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    private void checkCurrentValidation(Event event) {
        if (paymentRepository.existsByOrderIdAndTransactionId(
                event.getOrder().getOrderId(), event.getTransactionId())
        ) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
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
}