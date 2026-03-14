package com.learn.aisagaagent.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_diagnostics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaDiagnostic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String transactionId;
    @Column(columnDefinition = "TEXT")
    private String diagnosis;
    private LocalDateTime createdAt;
}