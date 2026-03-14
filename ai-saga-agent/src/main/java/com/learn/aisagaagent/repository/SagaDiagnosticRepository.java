package com.learn.aisagaagent.repository;

import com.learn.aisagaagent.model.SagaDiagnostic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaDiagnosticRepository extends JpaRepository<SagaDiagnostic, Long> {
    List<SagaDiagnostic> findByOrderId(String orderId);
    List<SagaDiagnostic> findAllByOrderByCreatedAtDesc();
}