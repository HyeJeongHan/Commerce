package com.hjhan.commerce.domain.order.controller;

import com.hjhan.commerce.domain.order.dto.OrderResponse;
import com.hjhan.commerce.domain.order.service.OrderService;
import com.hjhan.commerce.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok("주문이 완료되었습니다", orderService.createOrder(memberId));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getOrders(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(orderService.getOrders(memberId, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@AuthenticationPrincipal Long memberId,
                                               @PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(memberId, orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> payOrder(@AuthenticationPrincipal Long memberId,
                                               @PathVariable Long orderId) {
        return ApiResponse.ok("결제가 완료되었습니다", orderService.payOrder(memberId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@AuthenticationPrincipal Long memberId,
                                                  @PathVariable Long orderId) {
        return ApiResponse.ok("주문이 취소되었습니다", orderService.cancelOrder(memberId, orderId));
    }
}
