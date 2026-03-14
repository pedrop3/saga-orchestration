package com.learn.aisagaagent.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

public class TokenUsageListener implements ChatModelListener {

    @Override
    public void onResponse(ChatModelResponseContext context) {
        MetricsCollector collector = MetricsCollector.current();
        if (collector != null && context.chatResponse().tokenUsage() != null) {
            collector.recordTokenUsage(context.chatResponse().tokenUsage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) { /* optional logging */ }
}