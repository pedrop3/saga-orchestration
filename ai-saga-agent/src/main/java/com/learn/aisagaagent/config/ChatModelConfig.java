package com.learn.aisagaagent.config;

import com.learn.aisagaagent.listener.TokenUsageListener;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@Configuration
public class ChatModelConfig {

    private final AiProperties aiProperties;

    public ChatModelConfig(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Bean
    @Primary
    public ChatModel primaryChatModel() {
        return switch (aiProperties.getPrimaryModel()) {
            case "gemini" -> geminiChatModel();
            case "claude" -> claudeChatModel();
            case "ollama-no-think" -> ollamaChatModelWithoutThinking();
            default -> ollamaChatModel();
        };
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return createOllamaModel(true);
    }

    @Bean
    public OllamaChatModel ollamaChatModelWithoutThinking() {
        return createOllamaModel(false);
    }

    private OllamaChatModel createOllamaModel(boolean thinking) {
        AiProperties.ModelConfig config = aiProperties.getOllama();
        return OllamaChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .numPredict(512)
                .numCtx(32768)
                .repeatPenalty(1.2)
                .think(thinking)
                //.stop(endResponse)
                .listeners(List.of(new TokenUsageListener()))
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .build();
    }

    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        AiProperties.ModelConfig config = aiProperties.getGemini();
        return GoogleAiGeminiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .topP(0.8)
                .maxOutputTokens(1024)
                .thinkingConfig(GeminiThinkingConfig.builder()
                        .includeThoughts(true)
                        .thinkingBudget(512)
                        .build())
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .build();
    }

    @Bean
    public AnthropicChatModel claudeChatModel() {
        AiProperties.ModelConfig config = aiProperties.getClaude();
        return AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .build();
    }
}