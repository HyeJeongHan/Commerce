package com.hjhan.commerce.domain.member;

import com.hjhan.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@test.com",
                                "password", "password123",
                                "name", "테스터"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("중복 이메일로 가입 시 409 반환")
    void signup_duplicateEmail_returns409() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "email", "dup@test.com", "password", "password123", "name", "테스터"));

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body));

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("필수 값 누락 시 400 반환")
    void signup_missingField_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "bad-email",
                                "password", "123",       // 8자 미만
                                "name", "a"              // 2자 미만
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 토큰 반환")
    void login_success_returnsToken() throws Exception {
        // 회원가입 먼저
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "login@test.com", "password", "password123", "name", "로그인유저"))));

        // 로그인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "login@test.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 401 반환")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "pw@test.com", "password", "password123", "name", "유저"))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "pw@test.com",
                                "password", "wrongpassword"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("JWT 없이 내 정보 조회 시 401 반환")
    void getMyInfo_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT로 내 정보 조회 성공")
    void getMyInfo_withToken_success() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "me@test.com", "password", "password123", "name", "나의유저"))))
                .andExpect(status().isCreated());

        String token = extractToken("me@test.com", "password123");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me@test.com"));
    }

    // --- 헬퍼 ---

    protected String extractToken(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}
