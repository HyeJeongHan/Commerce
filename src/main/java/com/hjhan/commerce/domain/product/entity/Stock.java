package com.hjhan.commerce.domain.product.entity;

import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private Stock(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public static Stock create(Product product, int quantity) {
        return new Stock(product, quantity);
    }

    // 재고 차감 (주문 시 사용)
    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.quantity -= amount;
    }

    // 재고 복구 (주문 취소 시 사용)
    public void increase(int amount) {
        this.quantity += amount;
    }
}
