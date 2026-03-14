package com.learn.aisagaagent.provider;

import com.learn.aisagaagent.config.AiProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatModelProvider {

    private final Map<String, ChatModel> models;
    private final AiProperties aiProperties;

    public ChatModelProvider(
            AiProperties aiProperties,
            @Qualifier("ollamaChatModel") ChatModel ollama,
            @Qualifier("ollamaChatModelWithoutThinking") ChatModel ollamaWithoutThinking,
            @Qualifier("geminiChatModel") ChatModel gemini,
            @Qualifier("claudeChatModel") ChatModel claude) {

        this.aiProperties = aiProperties;
        this.models = Map.of(
                "ollama", ollama,
                "ollama-no-thinking", ollamaWithoutThinking,
                "gemini", gemini,
                "claude", claude
        );
    }

    public ChatModel getPrimaryModel() {
        String primaryModelName = aiProperties.getPrimaryModel();
        ChatModel model = models.get(primaryModelName);

        if (model == null) {
            // Log warning e fallback
            return models.get("ollama");
        }

        return model;
    }
}