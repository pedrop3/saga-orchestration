package com.learn.aisagaagent.listener;

import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class MetricsCollector {

    private static final ThreadLocal<MetricsCollector> CURRENT = new ThreadLocal<>();

    private final List<String> toolsUsed = new ArrayList<>();
    private final AtomicInteger steps = new AtomicInteger(0);
    private TokenUsage tokenUsage;

    public static MetricsCollector start() {
        MetricsCollector collector = new MetricsCollector();
        CURRENT.set(collector);
        return collector;
    }

    public static MetricsCollector current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public void recordTool(String toolName) {
        toolsUsed.add(toolName);
        steps.incrementAndGet();
    }

    public void recordTokenUsage(TokenUsage usage) {
        this.tokenUsage = usage;
    }

    public List<String> getToolsUsed()  { return Collections.unmodifiableList(toolsUsed); }
    public int getSteps()               { return steps.get(); }
    public TokenUsage getTokenUsage()   { return tokenUsage; }
}
