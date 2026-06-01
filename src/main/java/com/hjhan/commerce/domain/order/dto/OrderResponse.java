package com.hjhan.commerce.domain.order.dto;

import com.hjhan.commerce.domain.order.entity.Order;
import com.hjhan.commerce.domain.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String status,
        BigDecimal totalPrice,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public record OrderItemResponse(Long orderItemId, String productName,
                                    BigDecimal price, int quantity, BigDecimal subtotal) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(), item.getProductName(),
                    item.getPrice(), item.getQuantity(), item.getSubtotal()
            );
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
