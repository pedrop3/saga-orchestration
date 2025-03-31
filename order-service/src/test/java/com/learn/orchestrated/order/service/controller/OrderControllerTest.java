package com.learn.orchestrated.order.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequest;
import com.learn.orchestrated.order.service.service.OrderService;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Autowired
    ObjectMapper objectMapper;

    OrderRequest orderRequest;
    OrderDocument orderResponse;

    @BeforeEach
    void setUp() {
        orderRequest = buildOrderRequest();
        orderResponse = buildOrderResponse();
    }

    @Test
    void shouldCreateOrderSuccessfully_whenValidRequest() throws Exception {

        Mockito.when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderResponse.getOrderId()))
                .andExpect(jsonPath("$.products").isArray());

        Mockito.verify(orderService).createOrder(any(OrderRequest.class));
    }

    private OrderRequest buildOrderRequest() {
        OrderProducts product = new OrderProducts();
        product.setProduct(new Product());
        product.setQuantity(2);
        return new OrderRequest(List.of(product));
    }

    private OrderDocument buildOrderResponse() {
        OrderDocument doc = new OrderDocument();
        doc.setOrderId(UUID.randomUUID().toString());
        doc.setProducts(orderRequest.products());
        return doc;
    }
}
