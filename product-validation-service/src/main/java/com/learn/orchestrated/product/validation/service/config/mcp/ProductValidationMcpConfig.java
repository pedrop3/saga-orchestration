package com.learn.orchestrated.product.validation.service.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.orchestrated.product.validation.service.service.ProductValidationService;
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
import java.util.stream.Collectors;

@Configuration
public class ProductValidationMcpConfig {

    private static final String SERVER_NAME    = "product-validation-mcp";
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
            ProductValidationService productValidationService) {

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        checkProductExists(productValidationService),
                        checkValidationExists(productValidationService),
                        listCatalog(productValidationService)
                )
                .build();
    }

    private SyncToolSpecification checkProductExists(ProductValidationService productValidationService) {
        return tool(
                "checkProductExists",
                "Checks whether a product exists in the catalog by its product code. " +
                        "Use this tool to verify if a saga order references a valid product " +
                        "before investigating further validation failures.",
                """
                {
                  "type": "object",
                  "properties": {
                    "productCode": {
                      "type": "string",
                      "description": "Product code to verify. Available values: COMIC_BOOKS, BOOKS, MOVIES, MUSIC"
                    }
                  },
                  "required": ["productCode"]
                }
                """,
                args -> {
                    String code   = (String) args.get("productCode");
                    boolean found = productValidationService.existsByCode(code);
                    return found
                            ? "Product exists | productCode=" + code
                            : "Product not found | productCode=" + code;
                }
        );
    }

    private SyncToolSpecification checkValidationExists(ProductValidationService productValidationService) {
        return tool(
                "checkValidationExists",
                "Checks whether a product validation record exists for a given order and transaction. " +
                        "Use this tool to confirm that the product validation step of a saga was executed, " +
                        "or to diagnose missing validations that may have caused a downstream failure.",
                """
                {
                  "type": "object",
                  "properties": {
                    "orderId": {
                      "type": "string",
                      "description": "The unique identifier of the order."
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
                    boolean exists       = productValidationService.existsByOrderIdAndTransactionId(orderId, transactionId);
                    return exists
                            ? "Validation found | orderId=" + orderId + " | transactionId=" + transactionId
                            : "No validation found | orderId=" + orderId + " | transactionId=" + transactionId;
                }
        );
    }

    private SyncToolSpecification listCatalog(ProductValidationService productValidationService) {
        return tool(
                "listCatalog",
                "Returns all products currently available in the catalog. " +
                        "Use this tool to discover valid product codes that can be used " +
                        "in saga orders, or to verify catalog integrity.",
                """
                {
                  "type": "object",
                  "properties": {},
                  "required": []
                }
                """,
                args -> {
                    List<String> products = productValidationService.findAll()
                            .stream()
                            .map(p -> "code=" + p.getCode())
                            .collect(Collectors.toList());
                    return products.isEmpty()
                            ? "No products found in catalog."
                            : "Catalog:\n" + String.join("\n", products);
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