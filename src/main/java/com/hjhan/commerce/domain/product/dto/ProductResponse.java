package com.hjhan.commerce.domain.product.dto;

import com.hjhan.commerce.domain.product.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String status,
        String categoryName,
        int stockQuantity
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus().name(),
                product.getCategory().getName(),
                product.getStock() != null ? product.getStock().getQuantity() : 0
        );
    }
}
