package com.learn.aisagaagent.service;

import com.learn.aisagaagent.listener.MetricsCollector;
import com.learn.aisagaagent.provider.ChatMemoryProvider;
import com.learn.aisagaagent.service.agent.DataAnalystAgent;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataAnalystAgentService {

    private final ChatMemoryProvider chatMemoryProvider;
    private final ChatModel primaryChatModel;
    private final McpToolProvider mcpToolProvider;

    public String runAgent(String userQuestion) {
        MetricsCollector collector = MetricsCollector.start();
        long startTime = System.currentTimeMillis();
        String answer = null;

        try {

            DataAnalystAgent agentWithMemory = createAgentWithMemory("userID");
            answer = agentWithMemory.analyze(userQuestion);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MetricsCollector.clear();
            collectMetrics(startTime, collector);
        }

        return answer;
    }

    private DataAnalystAgent createAgentWithMemory(String userId) {
        ChatMemory memory = chatMemoryProvider.getMemory(userId);

        return AiServices.builder(DataAnalystAgent.class)
                .chatModel(primaryChatModel)
                .chatMemory(memory)
                .toolProvider(mcpToolProvider)
                .maxSequentialToolsInvocations(3)
                .build();
    }



    private void collectMetrics(long startTime, MetricsCollector collector) {
        long executionTime = System.currentTimeMillis() - startTime;
        TokenUsage usage = collector.getTokenUsage();
        // Log metrics aqui
    }
}