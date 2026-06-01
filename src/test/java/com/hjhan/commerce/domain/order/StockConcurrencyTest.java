package com.hjhan.commerce.domain.order;

import com.hjhan.commerce.domain.cart.dto.CartAddRequest;
import com.hjhan.commerce.domain.cart.service.CartService;
import com.hjhan.commerce.domain.member.entity.Member;
import com.hjhan.commerce.domain.member.repository.MemberRepository;
import com.hjhan.commerce.domain.order.service.OrderService;
import com.hjhan.commerce.domain.product.repository.StockRepository;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import com.hjhan.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class StockConcurrencyTest extends IntegrationTestSupport {

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final int STOCK = 5;
    private static final int THREADS = 10;

    private Long productId;
    private List<Long> memberIds;

    @BeforeEach
    void setUp() throws Exception {
        // 카테고리, 상품 생성 (재고 5개)
        var categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(Map.of("name", "테스트카테고리"))))
                .andReturn();
        Long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        var productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "한정판 상품",
                                "price", 10000,
                                "categoryId", categoryId,
                                "stockQuantity", STOCK
                        ))))
                .andReturn();
        productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 회원 10명 생성 + 각자 장바구니에 1개씩 담기
        memberIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Member member = memberRepository.save(
                    Member.create("user" + i + "@test.com",
                            passwordEncoder.encode("password123"),
                            "유저" + i));
            memberIds.add(member.getId());
            cartService.addItem(member.getId(), new CartAddRequest(productId, 1));
        }
    }

    @Test
    @DisplayName("재고 5개인 상품에 10명이 동시 주문하면 정확히 5명만 성공한다")
    void concurrentOrders_onlyStockCountSucceeds() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(THREADS);  // 모든 스레드 준비 대기
        CountDownLatch start = new CountDownLatch(1);         // 동시 출발 신호
        CountDownLatch done = new CountDownLatch(THREADS);    // 모든 스레드 완료 대기

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        for (Long memberId : memberIds) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();  // 신호 대기 (모두 동시에 출발)
                    orderService.createOrder(memberId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();   // 모든 스레드가 준비될 때까지 대기
        start.countDown(); // 동시 출발
        done.await();    // 모든 스레드 완료 대기
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(STOCK);
        assertThat(failCount.get()).isEqualTo(THREADS - STOCK);

        // DB 재고가 정확히 0인지 확인
        var stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getQuantity()).isZero();
    }
}
