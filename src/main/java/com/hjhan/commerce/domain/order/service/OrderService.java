package com.hjhan.commerce.domain.order.service;

import com.hjhan.commerce.domain.cart.entity.Cart;
import com.hjhan.commerce.domain.cart.entity.CartItem;
import com.hjhan.commerce.domain.cart.service.CartService;
import com.hjhan.commerce.domain.order.dto.OrderResponse;
import com.hjhan.commerce.domain.order.entity.OrderStatus;
import com.hjhan.commerce.domain.order.entity.Order;
import com.hjhan.commerce.domain.order.entity.OrderItem;
import com.hjhan.commerce.domain.order.repository.OrderRepository;
import com.hjhan.commerce.domain.product.entity.Stock;
import com.hjhan.commerce.domain.product.repository.StockRepository;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final CartService cartService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public OrderResponse createOrder(Long memberId) {
        Cart cart = cartService.getCartEntity(memberId);
        List<CartItem> cartItems = cart.getCartItems();

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        Order order = Order.create(cart.getMember(), BigDecimal.ZERO);
        orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            // L1 캐시에 이미 로드된 Stock 엔티티가 있을 수 있으므로 refresh로 강제 갱신 + 비관적 락 획득
            Stock stock = stockRepository
                    .findByProductId(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE);

            stock.decrease(cartItem.getQuantity());

            OrderItem orderItem = OrderItem.create(order, cartItem.getProduct(), cartItem.getQuantity());
            order.getOrderItems().add(orderItem);
            totalPrice = totalPrice.add(orderItem.getSubtotal());
        }

        // 총 금액 반영 후 장바구니 비우기
        order.applyTotalPrice(totalPrice);
        cartService.clearCart(memberId);

        return OrderResponse.from(order);
    }

    public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findAllByMemberIdWithItems(memberId, pageable)
                .map(OrderResponse::from);
    }

    public OrderResponse getOrder(Long memberId, Long orderId) {
        Order order = getOrderEntity(orderId);
        validateOwner(order, memberId);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse payOrder(Long memberId, Long orderId) {
        Order order = getOrderEntity(orderId);
        validateOwner(order, memberId);
        order.pay();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long memberId, Long orderId) {
        Order order = getOrderEntity(orderId);
        validateOwner(order, memberId);

        order.cancel(() -> {
            for (OrderItem item : order.getOrderItems()) {
                stockRepository.findByProductId(item.getProduct().getId()).ifPresent(stock -> {
                    entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE);
                    stock.increase(item.getQuantity());
                });
            }
        });

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderEntity(orderId);
        order.adminUpdateStatus(newStatus);
        return OrderResponse.from(order);
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateOwner(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

}
