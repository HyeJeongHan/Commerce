package com.hjhan.commerce.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
) {}
