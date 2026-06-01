package com.hjhan.commerce.domain.order.entity;

public enum OrderStatus {
    PENDING,    // 주문 완료 (결제 대기)
    PAID,       // 결제 완료
    SHIPPED,    // 배송 중
    DELIVERED,  // 배송 완료
    CANCELLED   // 취소
}
