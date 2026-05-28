package com.hjhan.commerce.domain.member.dto;

import com.hjhan.commerce.domain.member.entity.Member;

public record MemberResponse(Long id, String email, String name, String role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name()
        );
    }
}
