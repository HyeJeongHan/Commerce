package com.hjhan.commerce.domain.order;

import com.hjhan.commerce.domain.product.repository.StockRepository;
import com.hjhan.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends IntegrationTestSupport {

    @Autowired private StockRepository stockRepository;

    private String userToken;
    private String adminToken;
    private Long productId;

    @BeforeEach
    void setUp() throws Exception {
        // 일반 유저 생성
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "user@test.com", "password", "password123", "name", "유저"))));
        userToken = extractToken("user@test.com", "password123");

        // 관리자 생성 (카테고리/상품 등록용)
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "admin@test.com", "password", "admin1234", "name", "관리자"))));
        adminToken = extractToken("admin@test.com", "admin1234");

        // DB에서 직접 admin 권한 부여
        // (실제 서비스에서는 관리자 승격 API를 별도 구현)

        // 카테고리 생성 (admin 권한 필요 — 테스트용으로 SecurityConfig에 permitAll 추가 없이
        // 여기서는 DB 직접 세팅 방식 대신, 테스트 프로파일에서 permitAll 하거나
        // 실제로는 @WithMockUser를 활용)
        // 편의상 MockMvc에서 직접 admin Role 지정
        productId = createProductDirectly(adminToken);
    }

    @Test
    @DisplayName("장바구니에 상품을 담을 수 있다")
    void addToCart_success() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.totalPrice").value(3000000));
    }

    @Test
    @DisplayName("같은 상품을 다시 담으면 수량이 합산된다")
    void addToCart_sameProduct_quantityMerged() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2));

        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken).content(body));
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken).content(body));

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.data.items[0].quantity").value(4));
    }

    @Test
    @DisplayName("주문 생성 시 재고가 차감되고 장바구니가 비워진다")
    void createOrder_stockDecreasedAndCartCleared() throws Exception {
        // 장바구니 담기
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 3))));

        // 주문 생성
        mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").value(4500000))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        // 재고 확인 (10 - 3 = 7)
        var stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(7);

        // 장바구니 비워짐 확인
        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("빈 장바구니로 주문 시 실패한다")
    void createOrder_emptyCart_fails() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PENDING 상태 주문을 결제할 수 있다")
    void payOrder_pending_success() throws Exception {
        Long orderId = createOrder(3);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("이미 결제된 주문을 다시 결제하면 실패한다")
    void payOrder_alreadyPaid_fails() throws Exception {
        Long orderId = createOrder(2);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                .header("Authorization", "Bearer " + userToken));

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("주문 취소 시 재고가 복구된다")
    void cancelOrder_stockRestored() throws Exception {
        Long orderId = createOrder(3);

        var stockBefore = stockRepository.findByProductId(productId).orElseThrow().getQuantity();

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        var stockAfter = stockRepository.findByProductId(productId).orElseThrow().getQuantity();
        assertThat(stockAfter).isEqualTo(stockBefore + 3);
    }

    @Test
    @DisplayName("재고보다 많은 수량 주문 시 실패한다")
    void createOrder_insufficientStock_fails() throws Exception {
        // 재고 10개인데 20개 주문
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 20))));

        mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // 재고 변동 없음 확인 (트랜잭션 롤백)
        var stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(10);
    }

    // --- 헬퍼 ---

    private Long createProductDirectly(String token) throws Exception {
        // 테스트에서는 Security 우회 없이 withMockUser 방식 대신
        // 실제로 category/product 엔드포인트를 호출. Admin 권한이 필요하므로
        // 여기서는 MockMvc의 with(user(...)) 패턴 사용
        var categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(Map.of("name", "전자기기", "description", "전자제품"))))
                .andReturn();
        Long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        var productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "테스트 상품",
                                "price", 1500000,
                                "categoryId", categoryId,
                                "stockQuantity", 10
                        ))))
                .andReturn();
        return objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createOrder(int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity))));

        var result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderId").asLong();
    }

    private String extractToken(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
