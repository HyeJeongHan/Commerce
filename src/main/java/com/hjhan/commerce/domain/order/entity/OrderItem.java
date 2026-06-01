package com.hjhan.commerce.domain.order.entity;

import com.hjhan.commerce.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // 주문 시점의 상품명과 가격을 스냅샷으로 저장
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    private OrderItem(Order order, Product product, int quantity) {
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.price = product.getPrice();
        this.quantity = quantity;
    }

    public static OrderItem create(Order order, Product product, int quantity) {
        return new OrderItem(order, product, quantity);
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
