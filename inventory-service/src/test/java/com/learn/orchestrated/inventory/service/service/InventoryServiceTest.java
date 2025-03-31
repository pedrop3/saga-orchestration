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
import com.learn.sagacommons.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
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
    private Inventory inventory;

    @BeforeEach
    void setup() {
        event = createEvent("order-1", "tx-123", 2, "P123");
        inventory = createInventory("P123", 5);
    }

    @Test
    void shouldUpdateInventorySuccessfully() {
        mockInventoryFlow(true, true);
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.updateInventory(event);

        assertEquals(SagaStatusEnum.SUCCESS, event.getStatus(), "Status should be SUCCESS");
        assertEquals("INVENTORY_SERVICE", event.getSource());
        assertHistoryContains("Inventory updated successfully");
        verify(orderInventoryRepository).save(any(OrderInventory.class));
        verify(inventoryRepository).save(any(Inventory.class));
        verify(producer).sendEvent("event-json");
    }

    @Test
    void shouldFailUpdateWhenProductOutOfStock() {
        inventory.setAvailable(1);
        mockInventoryFlow(true, true);
        when(jsonUtil.toJson(event)).thenReturn(Optional.of("event-json"));

        inventoryService.updateInventory(event);

        assertEquals(ROLLBACK, event.getStatus());
        assertEquals("INVENTORY_SERVICE", event.getSource());
        assertHistoryContains("Fail to update inventory");
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
        assertHistoryContains("Rollback executed for inventory");
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
        assertHistoryContains("Rollback not executed for inventory");
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

    private Event createEvent(String orderId, String txId, int quantity, String productCode) {
        Product product = new Product();
        product.setCode(productCode);

        OrderProducts orderProduct = new OrderProducts();
        orderProduct.setProduct(product);
        orderProduct.setQuantity(quantity);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setProducts(List.of(orderProduct));

        Event event = new Event();
        event.setTransactionId(txId);
        event.setOrder(order);
        event.setEventHistory(new ArrayList<>());
        return event;
    }

    private Inventory createInventory(String productCode, int available) {
        Inventory inventory = new Inventory();
        inventory.setProductCode(productCode);
        inventory.setAvailable(available);
        return inventory;
    }

    private void mockInventoryFlow(boolean repoReturnsInventory, boolean allowTransaction) {
        when(orderInventoryRepository.existsByOrderIdAndTransactionId("order-1", "tx-123"))
                .thenReturn(!allowTransaction);

        if (repoReturnsInventory) {
            when(inventoryRepository.findByProductCode("P123"))
                    .thenReturn(Optional.of(inventory));
        } else {
            when(inventoryRepository.findByProductCode("P123"))
                    .thenReturn(Optional.empty());
        }
    }

    private void assertHistoryContains(String message) {
        assertTrue(event.getEventHistory().stream()
                        .anyMatch(h -> h.getMessage().toLowerCase().contains(message.toLowerCase())),
                "Expected history to contain: " + message);
    }
}
