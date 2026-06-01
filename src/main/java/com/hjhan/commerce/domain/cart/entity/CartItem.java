package com.hjhan.commerce.domain.cart.entity;

import com.hjhan.commerce.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private CartItem(Cart cart, Product product, int quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public static CartItem create(Cart cart, Product product, int quantity) {
        return new CartItem(cart, product, quantity);
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
