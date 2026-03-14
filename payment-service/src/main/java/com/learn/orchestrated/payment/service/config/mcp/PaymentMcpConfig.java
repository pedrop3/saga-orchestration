package com.learn.orchestrated.payment.service.config.mcp;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.payment.service.enums.PaymentStatus;
import com.learn.orchestrated.payment.service.service.PaymentService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class PaymentMcpConfig {

    private static final String SERVER_NAME    = "payment-mcp";
    private static final String SERVER_VERSION = "1.0.0";

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
        return new ServletRegistrationBean<>(transport, "/sse", "/mcp/message","/mcp");
    }

    @Bean
    public McpSyncServer mcpServer(
            HttpServletSseServerTransportProvider transport,
            PaymentService paymentService) {

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        getPaymentStatus(paymentService),
                        getRefundRate(paymentService),
                        getRecentRefunds(paymentService)
                )
                .build();
    }

    private SyncToolSpecification getPaymentStatus(PaymentService paymentService) {
        return tool(
                "getPaymentStatus",
                "Returns the current payment status for a given transaction. " +
                        "Use this tool to verify whether a payment was successfully processed, " +
                        "is still pending, or has been refunded as part of a saga compensation. " +
                        "orderId is optional — transactionId alone is sufficient.",
                """
                {
                  "type": "object",
                  "properties": {
                    "orderId": {
                      "type": "string",
                      "description": "Order ID (optional if transactionId is provided)."
                    },
                    "transactionId": {
                      "type": "string",
                      "description": "Transaction ID associated with the saga execution."
                    }
                  },
                  "required": ["transactionId"]
                }
                """,
                args -> {
                    String orderId       = (String) args.get("orderId");
                    String transactionId = (String) args.get("transactionId");
                    return paymentService.findByTransactionId(transactionId)
                            .map(p -> "orderId=" + orderId
                                    + " | transactionId=" + transactionId
                                    + " | status=" + p.getStatus()
                                    + " | totalAmount=" + p.getTotalAmount()
                                    + " | totalItems=" + p.getTotalItems())
                            .orElse("No payment found for orderId=" + orderId
                                    + " | transactionId=" + transactionId);
                }
        );
    }

    private SyncToolSpecification getRefundRate(PaymentService paymentService) {
        return tool(
                "getRefundRate",
                "Returns the overall refund rate as a percentage of total payments. " +
                        "Use this tool to assess the health of payment processing and identify " +
                        "whether saga compensations are occurring at an abnormal rate.",
                """
                {
                  "type": "object",
                  "properties": {},
                  "required": []
                }
                """,
                args -> {
                    long total    = paymentService.count();
                    long refunded = paymentService.countByStatus(PaymentStatus.REFUND);
                    if (total == 0) return "No payments found.";
                    double rate = (refunded * 100.0) / total;
                    return "totalPayments=" + total
                            + " | refunded=" + refunded
                            + " | refundRate=" + String.format("%.2f", rate) + "%";
                }
        );
    }

    private SyncToolSpecification getRecentRefunds(PaymentService paymentService) {
        return tool(
                "getRecentRefunds",
                "Returns the most recent refunded payments up to the given limit. " +
                        "Use this tool to investigate recent saga compensations and understand " +
                        "which orders triggered payment rollbacks.",
                """
                {
                  "type": "object",
                  "properties": {
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of refunds to return."
                    }
                  },
                  "required": ["limit"]
                }
                """,
                args -> {
                    int limit = (Integer) args.get("limit");
                    List<String> refunds = paymentService.findByStatusOrderByCreatedAtDesc(PaymentStatus.REFUND)
                            .stream()
                            .limit(limit)
                            .map(p -> "orderId=" + p.getOrderId()
                                    + " | transactionId=" + p.getTransactionId()
                                    + " | totalAmount=" + p.getTotalAmount())
                            .toList();
                    return refunds.isEmpty()
                            ? "No refunds found."
                            : "Recent refunds:\n" + String.join("\n", refunds);
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