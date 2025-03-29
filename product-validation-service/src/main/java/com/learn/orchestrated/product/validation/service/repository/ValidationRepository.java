package com.learn.orchestrated.product.validation.service.repository;

import com.learn.orchestrated.product.validation.service.model.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidationRepository extends JpaRepository<Validation, Integer> {

    boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);

    Optional<Validation> findByOrderIdAndTransactionId(String orderId, String transactionId);
}
