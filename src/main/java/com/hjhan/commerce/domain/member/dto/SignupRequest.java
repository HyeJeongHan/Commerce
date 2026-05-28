package com.hjhan.commerce.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        String password,

        @NotBlank @Size(min = 2, message = "이름은 2자 이상이어야 합니다")
        String name
) {}
