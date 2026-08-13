package com.zcommcx.member.web.dto;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponse(
        String name,
        String phone,
        Gender gender,
        Integer age,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getName(),
                member.getPhone(),
                member.getGender(),
                member.getAge(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
