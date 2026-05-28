package com.hjhan.commerce.domain.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank(message = "상품명은 필수입니다")
        String name,

        String description,

        @NotNull(message = "가격은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다")
        BigDecimal price,

        @NotNull(message = "카테고리는 필수입니다")
        Long categoryId
) {}
