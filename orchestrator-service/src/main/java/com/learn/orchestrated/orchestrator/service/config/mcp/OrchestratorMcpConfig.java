package com.learn.orchestrated.orchestrator.service.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.orchestrator.service.enums.TopicsEnum;
import com.learn.orchestrated.orchestrator.service.producer.SagaOrchestratorProducer;
import com.learn.orchestrated.orchestrator.service.saga.SagaHandler;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.History;
import com.learn.sagacommons.utils.JsonUtil;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.learn.sagacommons.enums.SagaStatusEnum.FAIL;

@Slf4j
@Configuration
public class OrchestratorMcpConfig {

    private static final String SERVER_NAME    = "orchestrator-mcp";
    private static final String SERVER_VERSION = "1.0.0";
    private static final String SOURCE         = "ORCHESTRATOR";


    @Bean
    public HttpServletSseServerTransportProvider mcpTransport() {
        return HttpServletSseServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServlet(
            HttpServletSseServerTransportProvider transport) {
        return new ServletRegistrationBean<>(transport, "/sse", "/mcp/message");
    }


    @Bean
    public McpSyncServer mcpServer(
            HttpServletSseServerTransportProvider transport,
            SagaOrchestratorProducer producer,
            JsonUtil jsonUtil) {

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        triggerCompensation(producer, jsonUtil),
                        getSagaRouteInfo()
                )
                .build();
    }

    private McpServerFeatures.SyncToolSpecification triggerCompensation(
            SagaOrchestratorProducer producer,
            JsonUtil jsonUtil) {

        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "orderId":       { "type": "string", "description": "Order ID to compensate" },
                    "transactionId": { "type": "string", "description": "Transaction ID to compensate" },
                    "reason":        { "type": "string", "description": "Reason for triggering compensation" }
                  },
                  "required": ["orderId", "transactionId", "reason"]
                }
                """;

        return tool(
                "triggerCompensation",
                "Triggers saga compensation (FINISH_FAIL) for a specific order/transaction. "
                        + "Use when the saga is stuck or needs manual rollback.",
                schema,
                args -> {
                    String orderId       = (String) args.get("orderId");
                    String transactionId = (String) args.get("transactionId");
                    String reason        = (String) args.get("reason");

                    // Constrói o Event mínimo que será consumido pelo SagaOrchestratedConsumer
                    // no tópico finish-fail → service.finishSagaFail(event)
                    Event event = new Event();
                    event.setEventId(UUID.randomUUID().toString());
                    event.setOrderId(orderId);
                    event.setTransactionId(transactionId);
                    event.setSource(SOURCE);
                    event.setStatus(FAIL);

                    History history = History.builder()
                            .source(SOURCE)
                            .status(FAIL.toString())
                            .message("Compensation triggered by AI agent: " + reason)
                            .createdAt(LocalDateTime.now())
                            .build();
                    event.addToHistory(history);

                    String payload = jsonUtil.toJson(event)
                            .orElseThrow(() -> new RuntimeException("Failed to serialize event"));

                    producer.sendEvent(TopicsEnum.FINISH_FAIL.getTopic(), payload);

                    log.info("[MCP] triggerCompensation → topic='{}' orderId={} transactionId={}",
                            TopicsEnum.FINISH_FAIL.getTopic(), orderId, transactionId);

                    return "Compensation triggered for orderId=%s transactionId=%s. Published to topic '%s'."
                            .formatted(orderId, transactionId, TopicsEnum.FINISH_FAIL.getTopic());
                }
        );
    }

    /**
     * Retorna a tabela de roteamento completa da saga (SagaHandler.SAGA_HANDLER).
     */
    private McpServerFeatures.SyncToolSpecification getSagaRouteInfo() {
        return tool(
                "getSagaRouteInfo",
                "Returns the full saga routing table: each (source, status) pair and the Kafka topic it routes to.",
                """
                        {
                          "type": "object",
                          "properties": {},
                          "required": []
                        }
                        """,
                args -> {
                    String routes = Arrays.stream(SagaHandler.SAGA_HANDLER)
                            .map(row -> "  source=%-30s status=%-10s → topic=%s"
                                    .formatted(
                                            row[SagaHandler.EVENT_SOURCE_INDEX],
                                            row[SagaHandler.SAGA_STATUS_INDEX],
                                            ((TopicsEnum) row[SagaHandler.TOPIC_INDEX]).getTopic()
                                    ))
                            .collect(Collectors.joining("\n"));

                    return "Saga routing table (%d entries):\n%s"
                            .formatted(SagaHandler.SAGA_HANDLER.length, routes);
                }
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification tool(String name, String description, String schema,
                                                         Function<Map<String, Object>, String> handler) {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(name, description, schema),
                (exchange, args) -> success(handler.apply(args))
        );
    }

    private McpSchema.CallToolResult success(String text) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(text)))
                .isError(false)
                .build();
    }
}