package com.learn.orchestrated.order.service.order.service;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequest;
import com.learn.orchestrated.order.service.exception.OrderProcessingException;
import com.learn.orchestrated.order.service.repository.OrderRepository;
import com.learn.orchestrated.order.service.service.EventPublisherService;
import com.learn.orchestrated.order.service.service.impl.OrderServiceImpl;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.dto.Product;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EventPublisherService eventPublisherService;
    @Mock private JsonUtil jsonUtil;

    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrderSuccessfully_whenRequestIsValid() {
        OrderRequest request = buildOrderRequest();
        OrderDocument savedOrder = buildOrderDocument(request);

        when(orderRepository.save(any(OrderDocument.class))).thenReturn(savedOrder);

        OrderDocument result = orderService.createOrder(request);

        assertNotNull(result);
        verify(orderRepository).save(any(OrderDocument.class));
        verify(eventPublisherService).publish(any(EventDocument.class));
    }

    @Test
    void shouldThrowOrderProcessingException_whenSavingFails() {
        OrderRequest request = buildOrderRequest();
        when(orderRepository.save(any(OrderDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("DB error"));

        OrderProcessingException exception = assertThrows(OrderProcessingException.class, () -> {
            orderService.createOrder(request);
        });

        assertEquals("Erro ao salvar ordem no banco de dados.", exception.getMessage());
        verify(orderRepository).save(any(OrderDocument.class));
        verify(eventPublisherService, never()).publish(any());
    }

    @Test
    void shouldThrowOrderProcessingException_whenPublishingFails() {
        OrderRequest request = buildOrderRequest();
        OrderDocument savedOrder = buildOrderDocument(request);
        savedOrder.setOrderId("order456");
        savedOrder.setTransactionId("tx456");

        when(orderRepository.save(any(OrderDocument.class))).thenReturn(savedOrder);
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventPublisherService).publish(any(EventDocument.class));

        OrderProcessingException exception = assertThrows(OrderProcessingException.class, () -> {
            orderService.createOrder(request);
        });

        assertEquals("Erro ao processar evento da ordem.", exception.getMessage());
        verify(orderRepository).save(any(OrderDocument.class));
        verify(eventPublisherService).publish(any(EventDocument.class));
    }

    private OrderRequest buildOrderRequest() {
        OrderProducts orderProduct = new OrderProducts();
        orderProduct.setProduct(new Product());
        orderProduct.setQuantity(1);
        return new OrderRequest(List.of(orderProduct));
    }

    private OrderDocument buildOrderDocument(OrderRequest request) {
        OrderDocument doc = new OrderDocument();
        doc.setOrderId(UUID.randomUUID().toString());
        doc.setProducts(request.products());
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }
}
