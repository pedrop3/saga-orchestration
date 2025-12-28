package com.learn.orchestrated.product.validation.service.service;

import com.learn.orchestrated.product.validation.service.model.Validation;
import com.learn.orchestrated.product.validation.service.producer.SagaProducer;
import com.learn.orchestrated.product.validation.service.repository.ProductValidationRepository;
import com.learn.orchestrated.product.validation.service.repository.ValidationRepository;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.dto.OrderProducts;
import com.learn.sagacommons.exception.ValidationException;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.learn.sagacommons.enums.SagaStatusEnum.*;
import static org.springframework.util.ObjectUtils.isEmpty;


@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final ProductValidationRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(Event event)  {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to validate product: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    private void validateProductsInformed(Event event) {
        if (isEmpty(event.getOrder()) || isEmpty(event.getOrder().getProducts())) {
            throw new ValidationException("Product list is empty!");
        }
        if (isEmpty(event.getOrder().getOrderId()) || isEmpty(event.getTransactionId())) {
            throw new ValidationException("OrderID and TransactionID must be informed!");
        }
    }

    private void checkCurrentValidation(Event event) {
        validateProductsInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(
                event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
        event.getOrder().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });
    }

    private void validateProductInformed(OrderProducts product) {
        if (isEmpty(product.getProduct()) || isEmpty(product.getProduct().getCode())) {
            throw new ValidationException("Product must be informed!");
        }
    }

    private void validateExistingProduct(String code) {
        if (!productRepository.existsByCode(code)) {
            throw new ValidationException("Product does not exists in database!");
        }
    }

    private void createValidation(Event event, boolean success) {
        var validation = Validation
                .builder()
                .orderId(event.getOrder().getOrderId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Products are validated successfully!");
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(String.valueOf(event.getStatus()))
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validate products: ".concat(message));
    }

    public void rollbackEvent(Event event) {
        changeValidationToFail(event);

        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Rollback executed on product validation!");

        producer.sendEvent(jsonUtil.toJson(event).orElseThrow());
    }

    private void changeValidationToFail(Event event) {
        validationRepository
                .findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .ifPresentOrElse(validation -> {
                            validation.setSuccess(false);
                            validationRepository.save(validation);
                        },
                        () -> createValidation(event, false));
    }
}