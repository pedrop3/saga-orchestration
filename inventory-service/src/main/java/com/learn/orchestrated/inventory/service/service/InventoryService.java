package com.learn.orchestrated.inventory.service.service;

import com.learn.orchestrated.inventory.service.model.Inventory;
import com.learn.orchestrated.inventory.service.model.OrderInventory;
import com.learn.orchestrated.inventory.service.producer.SagaProducer;
import com.learn.orchestrated.inventory.service.repository.InventoryRepository;
import com.learn.orchestrated.inventory.service.repository.OrderInventoryRepository;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.enums.SagaStatusEnum;
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.learn.sagacommons.enums.SagaStatusEnum.FAIL;
import static com.learn.sagacommons.enums.SagaStatusEnum.ROLLBACK;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(Event event) {
        try {
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getOrder());
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to update inventory: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    private void checkCurrentValidation(Event event) {
        if (orderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getOrder().getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }

    private void createOrderInventory(Event event) {
        event
                .getOrder()
                .getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
                });
    }

    private OrderInventory createOrderInventory(Event event,
                                                OrderProducts product,
                                                Inventory inventory) {
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getOrder().getOrderId())
                .transactionId(event.getTransactionId())
                .build();
    }

    private void updateInventory(Order order) {
        order.getProducts().forEach(product -> {
            var inventory = findInventoryByProductCode(product.getProduct().getCode());

            checkInventory(inventory.getAvailable(), product.getQuantity());
            inventory.setAvailable(inventory.getAvailable() - product.getQuantity());

            inventoryRepository.save(inventory);
        });
    }

    private void checkInventory(int available, int orderQuantity) {
        if (orderQuantity > available) {
            throw new ValidationException("Product is out of stock!");
        }
    }

    private void handleSuccess(Event event) {
        event.setStatus(SagaStatusEnum.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory updated successfully!");
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
        addHistory(event, "Fail to update inventory: ".concat(message));
    }

    public void rollbackInventory(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);

        try {
            returnInventoryToPreviousValues(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception ex) {
            addHistory(event, "Rollback not executed for inventory: ".concat(ex.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    private void returnInventoryToPreviousValues(Event event) {
        orderInventoryRepository
                .findByOrderIdAndTransactionId(event.getOrder().getOrderId(), event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());

                    inventoryRepository.save(inventory);
                    log.info("Restored inventory for order {}: from {} to {}",
                            event.getOrder().getOrderId(), orderInventory.getNewQuantity(), inventory.getAvailable());
                });
    }

    private Inventory findInventoryByProductCode(String productCode) {
        return inventoryRepository
                .findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found by informed product."));
    }
}
