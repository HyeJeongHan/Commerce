package com.hjhan.commerce.support;

import com.hjhan.commerce.domain.cart.repository.CartItemRepository;
import com.hjhan.commerce.domain.cart.repository.CartRepository;
import com.hjhan.commerce.domain.category.repository.CategoryRepository;
import com.hjhan.commerce.domain.member.repository.MemberRepository;
import com.hjhan.commerce.domain.order.repository.OrderRepository;
import com.hjhan.commerce.domain.product.repository.ProductRepository;
import com.hjhan.commerce.domain.product.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// Testcontainers 대신 로컬 commerce_test DB 사용
// Docker Desktop 소켓 호환 문제 회피 + 빠른 테스트 실행
@SpringBootTest
public abstract class IntegrationTestSupport {

    @Autowired private WebApplicationContext wac;
    @Autowired protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        stockRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
