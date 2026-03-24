package com.learn.orchestrated.order.service.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.repository.OrderRepository;
import com.learn.sagacommons.utils.JsonUtil;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class OrderMcpConfig {

    private static final String SERVER_NAME = "order-mcp";
    private static final String SERVER_VERSION = "1.0.0";
    private final JsonUtil jsonUtil;

    @Bean
    public HttpServletSseServerTransportProvider orderMcpTransport() {
        return HttpServletSseServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> orderMcpServlet(
            HttpServletSseServerTransportProvider transport) {
        return new ServletRegistrationBean<>(transport, "/sse", "/mcp/message");
    }

    @Bean
    public McpSyncServer orderMcpServer(
            HttpServletSseServerTransportProvider transport,
            OrderRepository orderRepository,
            EventRepository eventRepository) {

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        getOrderById(orderRepository),
                        getLastEventByOrder(eventRepository),
                        getLastEventByTransaction(eventRepository),
                        listRecentEvents(eventRepository)
                )
                .build();
    }

    private SyncToolSpecification getOrderById(OrderRepository orderRepository) {

        return tool(
                "getOrderById",
                "Returns the order details for a given orderId. " +
                        "Use this tool to inspect order data during saga troubleshooting.",
                """
                {
                  "type": "object",
                  "properties": {
                    "orderId": {
                      "type": "string",
                      "description": "Unique identifier of the order"
                    }
                  },
                  "required": ["orderId"]
                }
                """,
                args -> {

                    String orderId = (String) args.get("orderId");


                    return jsonUtil.toJson(orderRepository.findById(orderId)).orElse("No events found for orderId=" + orderId);
                }
        );
    }

    private SyncToolSpecification getLastEventByOrder(EventRepository eventRepository) {

        return tool(
                "getLastEventByOrder",
                "Returns the latest saga event for a given orderId. " +
                        "Useful to understand the current stage of the saga workflow.",
                """
                {
                  "type": "object",
                  "properties": {
                    "orderId": {
                      "type": "string",
                      "description": "Unique order identifier"
                    }
                  },
                  "required": ["orderId"]
                }
                """,
                args -> {

                    String orderId = (String) args.get("orderId");

                    Optional<EventDocument> event = eventRepository
                            .findTop1ByOrderIdOrderByCreatedAtDesc(orderId);

                    return jsonUtil.toJson(event).orElse("No events found for orderId=" + orderId);
                }
        );
    }

    private SyncToolSpecification getLastEventByTransaction(EventRepository eventRepository) {

        return tool(
                "getLastEventByTransaction",
                "Returns the latest saga event for a given transactionId. " +
                        "Useful to diagnose issues in distributed saga executions.",
                """
                {
                  "type": "object",
                  "properties": {
                    "transactionId": {
                      "type": "string",
                      "description": "Saga transaction identifier"
                    }
                  },
                  "required": ["transactionId"]
                }
                """,
                args -> {

                    String transactionId = (String) args.get("transactionId");

                    Optional<EventDocument> event = eventRepository
                            .findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId);

                    return jsonUtil.toJson(event).orElse("No events found for transactionId=" + transactionId);
                }
        );
    }

    private SyncToolSpecification listRecentEvents(EventRepository eventRepository) {

        return tool(
                "listRecentEvents",
                "Returns the most recent saga events ordered by creation time. " +
                        "Useful to inspect the system activity timeline.",
                """
                {
                  "type": "object",
                  "properties": {
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of events to return"
                    }
                  },
                  "required": ["limit"]
                }
                """,
                args -> {

                    int limit = (Integer) args.get("limit");

                    List<EventDocument> events = eventRepository.findAllByOrderByCreatedAtDesc()
                            .stream()
                            .limit(limit)
                            .toList();


                    return jsonUtil.toJson(events).orElse("No events found");
                }
        );
    }

    private SyncToolSpecification tool(String name,
                                       String description,
                                       String schema,
                                       Function<Map<String, Object>, String> handler) {

        return new SyncToolSpecification(
                new McpSchema.Tool(name, description, schema),
                (exchange, args) -> success(handler.apply(args))
        );
    }

    private CallToolResult success(String text) {

        return CallToolResult.builder()
                .content(List.of(new TextContent(text)))
                .isError(false)
                .build();
    }
}