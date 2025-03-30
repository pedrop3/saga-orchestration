package com.learn.orchestrated.inventory.service.service;

import com.learn.orchestrated.inventory.service.model.Inventory;
import com.learn.orchestrated.inventory.service.model.OrderInventory;
import com.learn.orchestrated.inventory.service.producer.SagaProducer;
import com.learn.orchestrated.inventory.service.repository.InventoryRepository;
import com.learn.orchestrated.inventory.service.repository.OrderInventoryRepository;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.dto.Product;
import com.learn.sagacommons.enums.SagaStatusEnum;
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static com.learn.sagacommons.enums.SagaStatusEnum.FAIL;
import static com.learn.sagacommons.enums.SagaStatusEnum.ROLLBACK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class InventoryServiceTest {

    @Mock private JsonUtil jsonUtil;
    @Mock private SagaProducer producer;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrderInventoryRepository orderInventoryRepository;

    @InjectMocks private InventoryService inventoryService;

    private Event event;
    private OrderProducts orderProduct;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        Product product = new Product();
        product.setCode("P123");

        orderProduct = new OrderProducts();
        orderProduct.setProduct(product);
        orderProduct.setQuantity(2);

        Order order = new Order();
        order.setOrderId("order-1");
        order.setProducts(List.of(orderProduct));

        event = new Event();
        event.setTransactionId("tx-123");
        event.setOrder(order);

        inventory = new Inventory();
        inventory.setAvailable(5);
    }

    @Test
    void shouldUpdateInventorySuccessfully() {
        when(orderInventoryRepository.existsByOrderIdAndTransactionId("order-1", "tx-123")).thenReturn(false);
        when(inventoryRepository.findByProductCode("P123")).thenReturn(Optional.of(inventory));
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.updateInventory(event);

        assertEquals(SagaStatusEnum.SUCCESS, event.getStatus());
        assertEquals("INVENTORY_SERVICE", event.getSource());
        verify(orderInventoryRepository).save(any(OrderInventory.class));
        verify(inventoryRepository).save(any(Inventory.class));
        verify(producer).sendEvent("event-json");
    }

    @Test
    void shouldFailUpdateWhenProductOutOfStock() {
        inventory.setAvailable(1);
        when(orderInventoryRepository.existsByOrderIdAndTransactionId("order-1", "tx-123")).thenReturn(false);
        when(inventoryRepository.findByProductCode("P123")).thenReturn(Optional.of(inventory));
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.updateInventory(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertEquals("INVENTORY_SERVICE", event.getSource());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("Fail to update inventory")));
        verify(producer).sendEvent("event-json");
    }

    @Test
    void shouldRollbackInventorySuccessfully() {
        OrderInventory orderInventory = OrderInventory.builder()
                .inventory(inventory)
                .oldQuantity(10)
                .newQuantity(5)
                .orderId("order-1")
                .transactionId("tx-123")
                .build();

        when(orderInventoryRepository.findByOrderIdAndTransactionId("order-1", "tx-123"))
                .thenReturn(List.of(orderInventory));
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.rollbackInventory(event);

        assertEquals(FAIL, event.getStatus());
        assertEquals("INVENTORY_SERVICE", event.getSource());
        assertEquals(10, inventory.getAvailable());
        verify(inventoryRepository).save(inventory);
        verify(producer).sendEvent("event-json");
    }

    @Test
    void shouldAddRollbackHistoryWhenExceptionDuringRollback() {
        when(orderInventoryRepository.findByOrderIdAndTransactionId(any(), any()))
                .thenThrow(new RuntimeException("DB error"));
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.rollbackInventory(event);

        assertEquals(FAIL, event.getStatus());
        assertTrue(event.getEventHistory().stream().anyMatch(h -> h.getMessage().contains("Rollback not executed")));
        verify(producer).sendEvent("event-json");
    }

    @Test
    void shouldThrowExceptionIfInventoryNotFound() {
        when(inventoryRepository.findByProductCode("P123"))
                .thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            inventoryService.updateInventory(event);
        });

        assertEquals("No value present", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionIfTransactionAlreadyExists() {
        when(orderInventoryRepository.existsByOrderIdAndTransactionId("order-1", "tx-123")).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> {
            inventoryService.updateInventory(event);
        });

        assertEquals("No value present", ex.getMessage());
    }
}