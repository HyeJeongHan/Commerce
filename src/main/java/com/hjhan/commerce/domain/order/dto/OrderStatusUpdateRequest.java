package com.hjhan.commerce.domain.order.dto;

import com.hjhan.commerce.domain.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
