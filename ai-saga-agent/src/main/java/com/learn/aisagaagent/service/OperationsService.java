package com.learn.aisagaagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learn.aisagaagent.model.SagaDiagnostic;
import com.learn.aisagaagent.repository.SagaDiagnosticRepository;
import com.learn.aisagaagent.service.agent.OperationsAgent;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.enums.SagaStatusEnum;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationsService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel primaryChatModel;
    private final SagaDiagnosticRepository diagnosticRepository;
    private ObjectMapper objectMapper;
    private OperationsAgent operationsAgent;
    private final ProfileClassifier profileClassifier;


    @PostConstruct
    void init() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Built once and reused — avoids creating a new instance per Kafka message
        this.operationsAgent = AiServices.builder(OperationsAgent.class)
                .chatModel(primaryChatModel)
                .build();

        log.info("[OperationsService] Virtual threads enabled: {}",
                Thread.currentThread().isVirtual());
    }

    @KafkaListener(
            groupId  = "${spring.kafka.consumer.group-id}",
            topics   = "${spring.kafka.topic.notify-ending}")
    public void onSagaEnded(String payload) {
        var event = parseEvent(payload).orElse(null);

        if (event == null) {
            log.warn("[OperationsService] Failed to parse event payload — skipping");
            return;
        }

        // Always vectorize — builds the historical base for RAG
        String historyText = buildHistoryText(event);
        vectorize(event, historyText);

        // Diagnose only on failure
        if (event.getStatus() == SagaStatusEnum.FAIL) {
            this.diagnose(event, historyText);
        }
    }

    private void diagnose(Event event, String historyText) {
        String ragContext = findSimilarIncidents(historyText);
        String prompt     = buildDiagnosticPrompt(event, historyText, ragContext);
        String diagnosis  = operationsAgent.analyze(prompt);

        log.info("[OperationsAgent] Diagnosis for orderId={}:\n{}", event.getOrderId(), diagnosis);

        diagnosticRepository.save(SagaDiagnostic.builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .diagnosis(diagnosis)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void vectorize(Event event, String historyText) {
        String profileKey = classifyProfile(event);

        var metadata = new Metadata()
                .put("orderId",   event.getOrderId())
                .put("status",    event.getStatus().toString())
                .put("profileKey", profileKey)
                .put("createdAt", LocalDateTime.now().toString());

        var segment = TextSegment.from(historyText, metadata);
        embeddingStore.add(embeddingModel.embed(segment).content(), segment);
    }

    private String classifyProfile(Event event) {
        if (event.getOrder() == null) return "default";
        return profileClassifier.classify(event.getOrder());
    }

    private String findSimilarIncidents(String historyText) {
        var queryEmbedding = embeddingModel.embed(historyText).content();
        var results = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(3)
                        .minScore(0.75)
                        .build());

        if (results.matches().isEmpty()) return "No similar incidents found in history.";

        return results.matches().stream()
                .map(m -> "--- Similar incident (score=" + String.format("%.2f", m.score()) + ") ---\n"
                        + m.embedded().text())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildDiagnosticPrompt(Event event, String history, String rag) {
        double totalAmount = event.getOrder() != null ? event.getOrder().getTotalAmount() : 0.0;
        return """
                SAGA FAILED — DIAGNOSE
                OrderId: %s | TransactionId: %s
                Final status: %s
                Total amount: R$ %.2f

                SAGA HISTORY:
                %s

                SIMILAR INCIDENTS (RAG):
                %s
                """.formatted(
                event.getOrderId(),
                event.getTransactionId(),
                event.getStatus(),
                totalAmount,
                history,
                rag);
    }

    private String buildHistoryText(Event event) {
        return event.getEventHistory().stream()
                .map(h -> h.getSource() + " [" + h.getStatus() + "]: " + h.getMessage())
                .collect(Collectors.joining("\n"));
    }

    private Optional<Event> parseEvent(String json) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, Event.class));
        } catch (JsonProcessingException ex) {
            log.error("[OperationsService] Failed to deserialize event: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}