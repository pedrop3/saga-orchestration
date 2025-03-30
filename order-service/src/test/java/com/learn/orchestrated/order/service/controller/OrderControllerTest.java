package com.learn.orchestrated.order.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequest;
import com.learn.orchestrated.order.service.service.OrderService;
import com.learn.sagacommons.dto.Order;
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
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequest orderRequest;

    private OrderDocument orderResponse;

    @BeforeEach
    void setUp() {
        OrderProducts orderProduct = new OrderProducts();
        orderProduct.setProduct(new Product());
        orderProduct.setQuantity(2);

        orderRequest = new OrderRequest(List.of(orderProduct));

        orderResponse = new OrderDocument();
        orderResponse.setOrderId(UUID.randomUUID().toString());
        orderResponse.setProducts(List.of(orderProduct));

    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        Mockito.when(orderService.createOrder(any(OrderRequest.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderResponse.getOrderId()))
                .andExpect(jsonPath("$.products").isArray());

        Mockito.verify(orderService).createOrder(any(OrderRequest.class));
    }
}