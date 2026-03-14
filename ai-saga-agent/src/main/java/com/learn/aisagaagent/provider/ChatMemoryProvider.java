package com.learn.aisagaagent.provider;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatMemoryProvider {

    private final Map<String, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    public ChatMemory getMemory(String userId) {
        return memoryStore.computeIfAbsent(userId,
                k -> MessageWindowChatMemory.withMaxMessages(10));
    }

    public void clearMemory(String userId) {
        memoryStore.remove(userId);
    }
}
