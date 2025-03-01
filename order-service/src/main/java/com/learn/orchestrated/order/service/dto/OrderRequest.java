package com.learn.orchestrated.order.service.dto;

import com.learn.sagacommons.dto.OrderProducts;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull @Schema(description = "Lista de produtos do pedido", example = "[{\"product\":{\"code\":\"COMIC_BOOKS\",\"unitValue\":15.50},\"quantity\":3},{\"product\":{\"code\":\"BOOKS\",\"unitValue\":9.90},\"quantity\":1}]")
        List<OrderProducts> products
) {}