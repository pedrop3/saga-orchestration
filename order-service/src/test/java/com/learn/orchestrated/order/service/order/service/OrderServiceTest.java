package com.learn.orchestrated.order.service.order.service;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private JsonUtil jsonUtil;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;


    @Test
    void shouldCreateOrder() {
        OrderProducts orderProducts = new OrderProducts();
        orderProducts.setProduct(new Product());
        orderProducts.setQuantity(1);

        OrderRequest orderRequest = new OrderRequest(List.of(orderProducts));

        OrderDocument orderDocument = new OrderDocument();
        orderDocument.setProducts(orderRequest.getProducts());
        orderDocument.setCreatedAt(LocalDateTime.now());
        orderDocument.setOrderId(UUID.randomUUID().toString());


        when(orderRepository.save(any(OrderDocument.class))).thenReturn(orderDocument);

        OrderDocument result = orderServiceImpl.createOrder(orderRequest);

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(OrderDocument.class));
        verify(eventPublisherService, times(1)).publish(any(EventDocument.class));

    }



}
