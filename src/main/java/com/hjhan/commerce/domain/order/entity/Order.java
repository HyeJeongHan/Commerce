package com.hjhan.commerce.domain.order.entity;

import com.hjhan.commerce.domain.member.entity.Member;
import com.hjhan.commerce.global.entity.BaseTimeEntity;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    private Order(Member member, BigDecimal totalPrice) {
        this.member = member;
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING;
    }

    public static Order create(Member member, BigDecimal totalPrice) {
        return new Order(member, totalPrice);
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_PAID);
        }
        this.status = OrderStatus.PAID;
    }

    public void applyTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void cancel(Runnable restoreStock) {
        if (this.status == OrderStatus.CANCELLED
                || this.status == OrderStatus.SHIPPED
                || this.status == OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        restoreStock.run();
        this.status = OrderStatus.CANCELLED;
    }
}
