package com.learn.aisagaagent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class EmbeddingConfig {

    @Value("${ai.ollama.base-url}")
    private String ollamaUrl;


    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaUrl)
                .modelName("nomic-embed-text") //nomic-embed-text via Ollama (local, gratuito)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("saga_history_embeddings")
                .dimension(768)   // nomic-embed-text output size
                .createTable(true)
                .build();
    }

    @Bean
    public EmbeddingStoreIngestor ingestor(
            EmbeddingModel model, EmbeddingStore<TextSegment> store) {
        return EmbeddingStoreIngestor.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .build();
    }
}