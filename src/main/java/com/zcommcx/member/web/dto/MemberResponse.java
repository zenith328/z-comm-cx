package com.zcommcx.member.web.dto;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponse(
        String name,
        String phone,
        Gender gender,
        Integer birthYear,
        // age는 birthYear로부터 매번 계산되는 파생값이다 — 기존에 age를 그대로 쓰던 화면(FO
        // 개인화 매칭 등)이 안 바뀌어도 되도록 유지하고, birthYear는 "내 정보 수정" 폼에서
        // 원래 입력값을 그대로 보여주기 위해 추가했다.
        Integer age,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getName(),
                member.getPhone(),
                member.getGender(),
                member.getBirthYear(),
                member.getAge(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
