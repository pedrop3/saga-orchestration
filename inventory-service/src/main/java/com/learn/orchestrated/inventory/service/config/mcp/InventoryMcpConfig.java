package com.learn.orchestrated.inventory.service.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.inventory.service.repository.OrderInventoryRepository;
import com.learn.orchestrated.inventory.service.service.InventoryService;
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
public class InventoryMcpConfig {

    private static final String SERVER_NAME    = "inventory-mcp";
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
        return new ServletRegistrationBean<>(transport, "/sse", "/mcp/message");
    }

    @Bean
    public McpSyncServer mcpServer(
            HttpServletSseServerTransportProvider transport,
            InventoryService inventoryService,
            OrderInventoryRepository orderInventoryRepository) {

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        getStockByProduct(inventoryService),
                        getLowStockAlert(inventoryService),
                        checkReservationExists(orderInventoryRepository)
                )
                .build();
    }


    private SyncToolSpecification getStockByProduct(InventoryService inventoryService) {
        return tool(
                "getStockByProduct",
                "Returns the current available stock quantity for a given product code. " +
                        "Use this tool to check whether a product has sufficient inventory before " +
                        "processing or validating a saga order.",
                """
                {
                  "type": "object",
                  "properties": {
                    "productCode": {
                      "type": "string",
                      "description": "Product code: COMIC_BOOKS, BOOKS, MOVIES, MUSIC"
                    }
                  },
                  "required": ["productCode"]
                }
                """,
                args -> {
                    String code = (String) args.get("productCode");
                    return inventoryService.findByProductCode(code)
                            .map(i -> "productCode=" + code + " | available=" + i.getAvailable())
                            .orElse("Product not found for productCode: " + code);
                }
        );
    }

    private SyncToolSpecification getLowStockAlert(InventoryService inventoryService) {
        return tool(
                "getLowStockAlert",
                "Returns a list of products whose available stock is below the given threshold. " +
                        "Use this tool to identify inventory risks that may cause saga failures, " +
                        "or to proactively detect products at risk of running out during order processing.",
                """
                {
                  "type": "object",
                  "properties": {
                    "threshold": {
                      "type": "integer",
                      "description": "Minimum stock level — products below this value will be returned."
                    }
                  },
                  "required": ["threshold"]
                }
                """,
                args -> {
                    int threshold = (Integer) args.get("threshold");
                    List<String> low = inventoryService.findByAvailableLessThan(threshold)
                            .stream()
                            .map(i -> i.getProductCode() + ": available=" + i.getAvailable())
                            .toList();
                    return low.isEmpty()
                            ? "No products found below threshold=" + threshold
                            : "Low stock products:\n" + String.join("\n", low);
                }
        );
    }

    private SyncToolSpecification checkReservationExists(OrderInventoryRepository orderInventoryRepository) {
        return tool(
                "checkReservationExists",
                "Checks whether an inventory reservation exists for a given order and transaction. " +
                        "Use this tool to verify if the inventory step of a saga was successfully executed, " +
                        "or to diagnose whether a rollback may be needed due to a missing reservation.",
                """
                {
                  "type": "object",
                  "properties": {
                    "orderId": {
                      "type": "string",
                      "description": "The unique identifier of the order to check."
                    },
                    "transactionId": {
                      "type": "string",
                      "description": "The unique transaction identifier associated with the saga execution."
                    }
                  },
                  "required": ["orderId", "transactionId"]
                }
                """,
                args -> {
                    String orderId       = (String) args.get("orderId");
                    String transactionId = (String) args.get("transactionId");
                    boolean exists       = orderInventoryRepository.existsByOrderIdAndTransactionId(orderId, transactionId);
                    return exists
                            ? "Reservation found | orderId=" + orderId + " | transactionId=" + transactionId
                            : "No reservation found | orderId=" + orderId + " | transactionId=" + transactionId;
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