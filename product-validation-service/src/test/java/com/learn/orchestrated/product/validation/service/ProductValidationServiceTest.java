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
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductValidationServiceTest {

    @Mock
    private JsonUtil jsonUtil;

    @Mock
    private SagaProducer producer;

    @Mock
    private ProductValidationRepository productRepository;

    @Mock
    private ValidationRepository validationRepository;

    @InjectMocks
    private ProductValidationService productValidationService;

    private Event event;
    private Order order;
    private OrderProducts orderProduct;
    private Product product;

    private static final String SUCCESS = "SUCCESS";
    private static final String ROLLBACK = "ROLLBACK";
    private static final String FAIL = "FAIL";
    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";


    @BeforeEach
    void setUp() {
        product = new Product("123",1);
        orderProduct = new OrderProducts(product, 1);
        order = Order.builder().orderId("orderId").products(List.of(orderProduct)).build();
        event = Event.builder().order(order).transactionId("transactionId").build();
        when(jsonUtil.toJson(any())).thenReturn(Optional.of("{}"));
    }

    @Test
    @DisplayName("Teste de validação bem-sucedida de produtos existentes")
    void validateExistingProducts_Success() {
        when(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())).thenReturn(false);
        when(productRepository.existsByCode(product.getCode())).thenReturn(true);

        productValidationService.validateExistingProducts(event);

        verify(validationRepository, times(1)).save(any(Validation.class));
        ArgumentCaptor<Validation> validationCaptor = ArgumentCaptor.forClass(Validation.class);
        verify(validationRepository).save(validationCaptor.capture());
        assertTrue(validationCaptor.getValue().isSuccess());
        assertEquals(event.getOrderId(), validationCaptor.getValue().getOrderId());
        assertEquals(event.getTransactionId(), validationCaptor.getValue().getTransactionId());
        assertEquals(SUCCESS, event.getStatus());
        assertEquals(CURRENT_SOURCE, event.getSource());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().equals("Products are validated successfully!")));
        verify(producer, times(1)).sendEvent("{}");
    }

    @Test
    @DisplayName("Teste de falha na validação de produtos existentes devido à lista de produtos vazia")
    void validateExistingProducts_Failure_EmptyProductList() {
        event.getOrder().setProducts(Collections.emptyList());

        assertThrows(ValidationException.class, () -> productValidationService.validateExistingProducts(event), "Product list is empty!");
        verify(validationRepository, never()).save(any());
        verify(producer, never()).sendEvent(anyString());
    }

    @Test
    @DisplayName("Teste de falha na validação de produtos existentes devido a informações de pedido em falta")
    void validateExistingProducts_Failure_MissingOrderInformation() {
        event.getOrder().setOrderId(null);

        assertThrows(ValidationException.class, () -> productValidationService.validateExistingProducts(event), "OrderID and TransactionID must be informed!");
        verify(validationRepository, never()).save(any());
        verify(producer, never()).sendEvent(anyString());
    }

    @Test
    @DisplayName("Teste de falha na validação de produtos existentes devido a informações de transação em falta")
    void validateExistingProducts_Failure_MissingTransactionInformation() {
        event.setTransactionId(null);

        assertThrows(ValidationException.class, () -> productValidationService.validateExistingProducts(event), "OrderID and TransactionID must be informed!");
        verify(validationRepository, never()).save(any());
        verify(producer, never()).sendEvent(anyString());
    }

    @Test
    @DisplayName("Teste de falha na validação de produtos existentes devido a registro de validação existente")
    void validateExistingProducts_Failure_ValidationRecordExists() {
        when(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())).thenReturn(true);

        assertThrows(ValidationException.class, () -> productValidationService.validateExistingProducts(event), "There's another transactionId for this validation.");
        verify(validationRepository, never()).save(any());
        verify(producer, never()).sendEvent(anyString());
    }

    @Test
    @DisplayName("Teste de falha na validação de produtos existentes devido a produto inexistente")
    void validateExistingProducts_Failure_ProductDoesNotExist() {
        when(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())).thenReturn(false);
        when(productRepository.existsByCode(product.getCode())).thenReturn(false);

        assertThrows(ValidationException.class, () -> productValidationService.validateExistingProducts(event), "Product does not exists in database!");
        verify(validationRepository, times(1)).save(any(Validation.class));
        ArgumentCaptor<Validation> validationCaptor = ArgumentCaptor.forClass(Validation.class);
        verify(validationRepository).save(validationCaptor.capture());
        assertFalse(validationCaptor.getValue().isSuccess());
        assertEquals(ROLLBACK, event.getStatus());
        assertEquals(CURRENT_SOURCE, event.getSource());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("Fail to validate products: Product does not exists in database!")));
        verify(producer, times(1)).sendEvent("{}");
    }

    @Test
    @DisplayName("Teste de rollback bem-sucedido com registro de validação existente")
    void rollbackEvent_Success_ExistingValidation() {
        Validation existingValidation = Validation.builder().orderId(event.getOrderId()).transactionId(event.getTransactionId()).success(true).build();
        when(validationRepository.findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())).thenReturn(Optional.of(existingValidation));

        productValidationService.rollbackEvent(event);

        verify(validationRepository, times(1)).findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId());
        verify(validationRepository, times(1)).save(existingValidation);
        assertFalse(existingValidation.isSuccess());
        assertEquals(FAIL, event.getStatus());
        assertEquals(CURRENT_SOURCE, event.getSource());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().equals("Rollback executed on product validation!")));
        verify(producer, times(1)).sendEvent("{}");
    }

    @Test
    @DisplayName("Teste de rollback bem-sucedido sem registro de validação existente")
    void rollbackEvent_Success_NoExistingValidation() {
        when(validationRepository.findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())).thenReturn(Optional.empty());

        productValidationService.rollbackEvent(event);

        verify(validationRepository, times(1)).findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId());
        verify(validationRepository, times(1)).save(any(Validation.class));
        ArgumentCaptor<Validation> validationCaptor = ArgumentCaptor.forClass(Validation.class);
        verify(validationRepository).save(validationCaptor.capture());
        assertFalse(validationCaptor.getValue().isSuccess());
        assertEquals(FAIL, event.getStatus());
        assertEquals(CURRENT_SOURCE, event.getSource());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().equals("Rollback executed on product validation!")));
        verify(producer, times(1)).sendEvent("{}");
    }




}
