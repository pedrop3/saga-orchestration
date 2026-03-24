package com.learn.aisagaagent.service;

import com.learn.aisagaagent.listener.MetricsCollector;
import com.learn.aisagaagent.provider.ChatMemoryProvider;
import com.learn.aisagaagent.service.agent.OperationsAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationAgentService {

    private final ChatMemoryProvider chatMemoryProvider;
    private final ChatModel primaryChatModel;

    public String runAgent(String userQuestion) {
        MetricsCollector collector = MetricsCollector.start();
        long startTime = System.currentTimeMillis();
        String answer = null;

        try {

            var operationAgent = createAgent();
            answer = operationAgent.analyze(userQuestion);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MetricsCollector.clear();
            collectMetrics(startTime, collector);
        }

        return answer;
    }

    private OperationsAgent createAgent() {
         return AiServices.builder(OperationsAgent.class)
                .chatModel(primaryChatModel)
                .maxSequentialToolsInvocations(3)
                .build();
    }

    private void collectMetrics(long startTime, MetricsCollector collector) {
        long executionTime = System.currentTimeMillis() - startTime;
        TokenUsage usage = collector.getTokenUsage();
        // Log metrics aqui
    }
}