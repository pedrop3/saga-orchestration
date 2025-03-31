package com.learn.orchestrated.payment.service.service;

import com.learn.orchestrated.payment.service.enums.PaymentStatus;
import com.learn.orchestrated.payment.service.model.Payment;
import com.learn.orchestrated.payment.service.producer.SagaProducer;
import com.learn.orchestrated.payment.service.respository.PaymentRepository;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.dto.Product;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.learn.sagacommons.enums.SagaStatusEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String ORDER_ID = "ORDER-001";
    private static final String TX_ID = "TX-001";

    @Mock private JsonUtil jsonUtil;
    @Mock private SagaProducer producer;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks private PaymentService paymentService;

    private Event event;
    private Payment payment;

    @BeforeEach
    void setUp() {
        event = buildEvent(10.0, 2);
        payment = buildPayment(20.0, 2);
    }

    @Test
    void shouldRealizePaymentSuccessfully_givenValidOrderAndAmount() {
        // Arrange
        givenNoExistingPayment();
        givenPaymentFound();
        givenJsonSerialization();

        // Act
        paymentService.realizePayment(event);

        // Assert
        assertEquals(SUCCESS, event.getStatus());
        assertEquals("PAYMENT_SERVICE", event.getSource());
        assertEquals(20.0, event.getOrder().getTotalAmount());
        assertEquals(2, event.getOrder().getTotalItems());
        assertHistoryContains("Payment realized successfully");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_givenAmountIsLessThanMinimum() {
        event = buildEvent(0.0, 1);
        payment = buildPayment(0.0, 1);
        givenNoExistingPayment();
        givenPaymentFound();
        givenJsonSerialization();

        paymentService.realizePayment(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("minimal amount");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_givenTransactionAlreadyExists() {

        when(paymentRepository.existsByOrderIdAndTransactionId(any(), any())).thenReturn(true);
        givenJsonSerialization();

        paymentService.realizePayment(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("transactionId");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_givenPaymentNotFound() {

        givenNoExistingPayment();
        when(paymentRepository.findByOrderIdAndTransactionId(any(), any())).thenReturn(Optional.empty());
        givenJsonSerialization();

        paymentService.realizePayment(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("Payment not found");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRealizeRefund_whenPaymentExists() {
        when(paymentRepository.findByOrderIdAndTransactionId(any(), any())).thenReturn(Optional.of(payment));
        givenJsonSerialization();

        paymentService.realizeRefund(event);

        assertEquals(FAIL, event.getStatus());
        assertEquals(PaymentStatus.REFUND, payment.getStatus());
        assertHistoryContains("Rollback executed for payment");
        verify(paymentRepository).save(payment);
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldHandleRefundFailureGracefully_whenPaymentNotFound() {
        when(paymentRepository.findByOrderIdAndTransactionId(any(), any()))
                .thenThrow(new RuntimeException("DB error"));
        givenJsonSerialization();


        paymentService.realizeRefund(event);

        assertEquals(FAIL, event.getStatus());
        assertHistoryContains("Rollback not executed for payment");
        verify(producer).sendEvent("{json}");
    }


    private Event buildEvent(double unitValue, int quantity) {
        Product product = new Product();
        product.setCode("PROD-1");
        product.setUnitValue(unitValue);

        OrderProducts orderProduct = new OrderProducts();
        orderProduct.setProduct(product);
        orderProduct.setQuantity(quantity);

        var order = new com.learn.sagacommons.dto.Order();
        order.setOrderId(ORDER_ID);
        order.setProducts(List.of(orderProduct));

        Event event = new Event();
        event.setOrder(order);
        event.setTransactionId(TX_ID);
        event.setEventHistory(new ArrayList<>());
        return event;
    }

    private Payment buildPayment(double totalAmount, int totalItems) {
        return Payment.builder()
                .orderId(ORDER_ID)
                .transactionId(TX_ID)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }


    private void givenNoExistingPayment() {
        when(paymentRepository.existsByOrderIdAndTransactionId(any(), any())).thenReturn(false);
    }

    private void givenPaymentFound() {
        when(paymentRepository.findByOrderIdAndTransactionId(any(), any())).thenReturn(Optional.of(payment));
    }

    private void givenJsonSerialization() {
        when(jsonUtil.toJson(any())).thenReturn(Optional.of("{json}"));
    }

    private void assertHistoryContains(String expectedMessage) {
        assertTrue(event.getEventHistory().stream()
                        .anyMatch(h -> h.getMessage().toLowerCase().contains(expectedMessage.toLowerCase())),
                "Expected message not found in history: " + expectedMessage);
    }
}
