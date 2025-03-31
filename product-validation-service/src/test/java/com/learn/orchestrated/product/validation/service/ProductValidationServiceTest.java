package com.learn.orchestrated.product.validation.service;

import com.learn.orchestrated.product.validation.service.model.Validation;
import com.learn.orchestrated.product.validation.service.producer.SagaProducer;
import com.learn.orchestrated.product.validation.service.repository.ProductValidationRepository;
import com.learn.orchestrated.product.validation.service.repository.ValidationRepository;
import com.learn.orchestrated.product.validation.service.service.ProductValidationService;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
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
class ProductValidationServiceTest {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    @Mock private JsonUtil jsonUtil;
    @Mock private SagaProducer producer;
    @Mock private ProductValidationRepository productRepository;
    @Mock private ValidationRepository validationRepository;

    @InjectMocks private ProductValidationService service;

    private Event event;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("123", 1);
        event = buildEvent("orderId", "transactionId", product.getCode(), 1);
        when(jsonUtil.toJson(any())).thenReturn(Optional.of("{json}"));
    }

    @Test
    void shouldValidateSuccessfully_whenProductExists() {
        givenNoValidationConflict();
        givenProductExists();

        service.validateExistingProducts(event);

        assertEquals(SUCCESS, event.getStatus());
        assertEquals(CURRENT_SOURCE, event.getSource());
        assertHistoryContains("Products are validated successfully!");
        verify(validationRepository).save(any(Validation.class));
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_whenProductDoesNotExist() {
        givenNoValidationConflict();
        when(productRepository.existsByCode(product.getCode())).thenReturn(false);

        service.validateExistingProducts(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("Product does not exists in database!");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_whenTransactionAlreadyExists() {
        when(validationRepository.existsByOrderIdAndTransactionId(any(), any())).thenReturn(true);

        service.validateExistingProducts(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("There's another transactionId for this validation.");
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollback_whenOrderIsEmpty() {
        event.setOrder(null);

        service.validateExistingProducts(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("Product list is empty!");
    }

    @Test
    void shouldRollback_whenProductIsNull() {
        event.getOrder().getProducts().getFirst().setProduct(null);

        service.validateExistingProducts(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("Product must be informed!");
    }

    @Test
    void shouldRollback_whenOrderIdOrTransactionIdMissing() {
        event.getOrder().setOrderId("");
        event.setTransactionId(null);

        service.validateExistingProducts(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertHistoryContains("OrderID and TransactionID must be informed!");
    }

    @Test
    void shouldRollbackAndUpdateValidation_whenItExists() {
        var validation = Validation.builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .success(true)
                .build();

        when(validationRepository.findByOrderIdAndTransactionId(any(), any()))
                .thenReturn(Optional.of(validation));

        service.rollbackEvent(event);

        assertEquals(FAIL, event.getStatus());
        assertFalse(validation.isSuccess());
        assertHistoryContains("Rollback executed on product validation!");
        verify(validationRepository).save(validation);
        verify(producer).sendEvent("{json}");
    }

    @Test
    void shouldRollbackAndCreateValidation_whenNotExists() {
        when(validationRepository.findByOrderIdAndTransactionId(any(), any()))
                .thenReturn(Optional.empty());

        service.rollbackEvent(event);

        assertEquals(FAIL, event.getStatus());
        assertHistoryContains("Rollback executed on product validation!");
        verify(validationRepository).save(argThat(v -> !v.isSuccess()));
        verify(producer).sendEvent("{json}");
    }


    private Event buildEvent(String orderId, String txId, String productCode, int quantity) {
        var prod = new Product(productCode, 1);
        var orderProduct = new OrderProducts(prod, quantity);
        var order = Order.builder().orderId(orderId).products(List.of(orderProduct)).build();
        return Event.builder()
                .order(order)
                .transactionId(txId)
                .eventHistory(new ArrayList<>())
                .build();
    }

    // ========= Helpers ==========

    private void givenNoValidationConflict() {
        when(validationRepository.existsByOrderIdAndTransactionId(any(), any())).thenReturn(false);
    }

    private void givenProductExists() {
        when(productRepository.existsByCode(product.getCode())).thenReturn(true);
    }

    private void assertHistoryContains(String expected) {
        assertTrue(event.getEventHistory().stream()
                        .anyMatch(h -> h.getMessage().toLowerCase().contains(expected.toLowerCase())),
                "Expected to find in history: " + expected);
    }
}

