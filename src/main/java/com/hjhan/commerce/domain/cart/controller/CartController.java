package com.hjhan.commerce.domain.cart.controller;

import com.hjhan.commerce.domain.cart.dto.CartAddRequest;
import com.hjhan.commerce.domain.cart.dto.CartItemUpdateRequest;
import com.hjhan.commerce.domain.cart.dto.CartResponse;
import com.hjhan.commerce.domain.cart.service.CartService;
import com.hjhan.commerce.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok(cartService.getCart(memberId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal Long memberId,
                                             @RequestBody @Valid CartAddRequest request) {
        return ApiResponse.ok(cartService.addItem(memberId, request));
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> updateItemQuantity(@AuthenticationPrincipal Long memberId,
                                                        @PathVariable Long cartItemId,
                                                        @RequestBody @Valid CartItemUpdateRequest request) {
        return ApiResponse.ok(cartService.updateItemQuantity(memberId, cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@AuthenticationPrincipal Long memberId,
                           @PathVariable Long cartItemId) {
        cartService.removeItem(memberId, cartItemId);
    }
}
