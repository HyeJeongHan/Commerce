package com.hjhan.commerce.domain.cart.service;

import com.hjhan.commerce.domain.cart.dto.CartAddRequest;
import com.hjhan.commerce.domain.cart.dto.CartResponse;
import com.hjhan.commerce.domain.cart.entity.Cart;
import com.hjhan.commerce.domain.cart.entity.CartItem;
import com.hjhan.commerce.domain.cart.repository.CartItemRepository;
import com.hjhan.commerce.domain.cart.repository.CartRepository;
import com.hjhan.commerce.domain.member.entity.Member;
import com.hjhan.commerce.domain.member.repository.MemberRepository;
import com.hjhan.commerce.domain.product.entity.Product;
import com.hjhan.commerce.domain.product.entity.ProductStatus;
import com.hjhan.commerce.domain.product.repository.ProductRepository;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public CartResponse getCart(Long memberId) {
        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElse(Cart.create(getMember(memberId)));
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse addItem(Long memberId, CartAddRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElseGet(() -> cartRepository.save(Cart.create(getMember(memberId))));

        // 이미 담긴 상품이면 수량만 변경, 없으면 컬렉션에 추가 (양방향 동기화)
        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        item -> item.updateQuantity(item.getQuantity() + request.quantity()),
                        () -> {
                            CartItem newItem = CartItem.create(cart, product, request.quantity());
                            cart.getCartItems().add(newItem);
                            cartItemRepository.save(newItem);
                        }
                );

        return CartResponse.from(cart);
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long memberId) {
        cartRepository.findByMemberIdWithItems(memberId).ifPresent(Cart::clear);
    }

    public Cart getCartEntity(Long memberId) {
        return cartRepository.findByMemberIdWithItems(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPTY_CART));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
