package com.learn.orchestrated.payment.service.respository;

import com.learn.orchestrated.payment.service.enums.PaymentStatus;
import com.learn.orchestrated.payment.service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);
    Optional<Payment> findByOrderIdAndTransactionId(String orderId, String transactionId);
    long countByStatus(PaymentStatus paymentStatus);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);
}