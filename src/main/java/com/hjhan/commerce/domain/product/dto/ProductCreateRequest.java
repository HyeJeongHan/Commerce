package com.hjhan.commerce.domain.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "상품명은 필수입니다")
        String name,

        String description,

        @NotNull(message = "가격은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다")
        BigDecimal price,

        @NotNull(message = "카테고리는 필수입니다")
        Long categoryId,

        @Min(value = 0, message = "재고는 0 이상이어야 합니다")
        int stockQuantity
) {}
