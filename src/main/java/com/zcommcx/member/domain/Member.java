package com.zcommcx.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FO 진입(로그인) 시 입력받는 이름/전화번호에 성별·출생년도(둘 다 선택 입력)를 추가로 붙여 저장하는
 * 경량 회원 정보. 별도의 계정/비밀번호 개념은 없고, 이름+전화번호로 동일인 여부를 판별한다.
 * 성별·출생년도는 향후 고객 성향 세그먼트(성별×연령별 상품 상세설명 노출 등)에 사용할 목적으로 받는다.
 * "연령"이 아니라 "출생년도"를 저장하는 이유: 연령은 입력 시점의 스냅샷이라 회원이 다시 갱신하지
 * 않으면 시간이 지날수록 stale해진다. 출생년도는 불변값이라 갱신 없이도 나이가 항상 정확하다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "member")
@IdClass(MemberId.class)
public class Member {

    @Id
    @Column(nullable = false)
    private String name;

    @Id
    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private Integer birthYear;

    /**
     * 체형(키/몸무게)은 둘 다 선택 입력이며, "내 체형 맞춤 핏 요약" 기능에서만 쓰인다 — 로그인
     * 회원이 상품상세에서 자기 체형에 맞는 리뷰만 골라 AI 요약봇에 물어볼 수 있게 자동으로
     * 질의문을 채워주는 용도. 세그먼트/개인화 매칭에는 관여하지 않는다(그건 성별/연령 몫).
     */
    private Integer heightCm;

    private Integer weightKg;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Member(String name, String phone, Gender gender, Integer birthYear, Integer heightCm, Integer weightKg) {
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.birthYear = birthYear;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(Gender gender, Integer birthYear, Integer heightCm, Integer weightKg) {
        this.gender = gender;
        this.birthYear = birthYear;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.updatedAt = LocalDateTime.now();
    }

    /** 출생년도 기준 현재 나이(연 나이: 현재연도 - 출생년도). 출생년도 미입력이면 null. */
    public Integer getAge() {
        return birthYear == null ? null : LocalDate.now().getYear() - birthYear;
    }
}
