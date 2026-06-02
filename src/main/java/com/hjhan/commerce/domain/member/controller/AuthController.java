package com.hjhan.commerce.domain.member.controller;

import com.hjhan.commerce.domain.member.dto.*;
import com.hjhan.commerce.domain.member.service.MemberService;
import com.hjhan.commerce.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ApiResponse.ok("회원가입이 완료되었습니다", memberService.signup(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(memberService.login(request));
    }

    @GetMapping("/members/me")
    public ApiResponse<MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok(memberService.getMyInfo(memberId));
    }

    @PutMapping("/members/password")
    @ResponseStatus(NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Long memberId,
                               @RequestBody @Valid PasswordChangeRequest request) {
        memberService.changePassword(memberId, request);
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.ok(memberService.refresh(request));
    }
}
