package com.hjhan.commerce.domain.cart.dto;

import com.hjhan.commerce.domain.cart.entity.Cart;
import com.hjhan.commerce.domain.cart.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, BigDecimal totalPrice) {

    public record CartItemResponse(Long cartItemId, Long productId, String productName,
                                   BigDecimal price, int quantity, BigDecimal subtotal) {
        public static CartItemResponse from(CartItem item) {
            BigDecimal subtotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getPrice(),
                    item.getQuantity(),
                    subtotal
            );
        }
    }

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(CartItemResponse::from)
                .toList();
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalPrice);
    }
}
