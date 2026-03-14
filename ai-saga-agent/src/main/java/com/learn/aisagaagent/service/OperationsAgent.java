package com.learn.aisagaagent.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface OperationsAgent {


    @Agent(
            name = "OperationsAgent",
            description = "Diagnoses FAIL sagas using RAG over historical incidents"
    )
    @SystemMessage("""
        You are a failure diagnosis specialist for distributed sagas.
        You will receive the full history of a FAIL saga and similar past incidents (RAG).
        Your only function is to produce a structured diagnostic report.
        
        ## Required response format
        ROOT CAUSE: <identify the service and reason>
        AFFECTED SERVICES: <list>
        FINANCIAL IMPACT: <estimate based on totalAmount>
        HISTORICAL PATTERN: <if RAG found similar cases, describe them>
        RECOMMENDATION: <specific corrective action>
        
        ## Rules
        1. Base your analysis only on the provided context — never invent data.
        2. If RAG found no similar incidents, state it explicitly.
        3. Be concise — the report will be consumed by a monitoring system.
        4. Respond in English.
        """)
    String analyze(@UserMessage String userQuestion);
}
