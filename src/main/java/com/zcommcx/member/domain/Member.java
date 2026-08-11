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

import java.time.LocalDateTime;

/**
 * FO 진입(로그인) 시 입력받는 이름/전화번호에 성별·연령(둘 다 선택 입력)을 추가로 붙여 저장하는
 * 경량 회원 정보. 별도의 계정/비밀번호 개념은 없고, 이름+전화번호로 동일인 여부를 판별한다.
 * 성별·연령은 향후 고객 성향 세그먼트(성별×연령별 상품 상세설명 노출 등)에 사용할 목적으로 받는다.
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

    private Integer age;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Member(String name, String phone, Gender gender, Integer age) {
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(Gender gender, Integer age) {
        this.gender = gender;
        this.age = age;
        this.updatedAt = LocalDateTime.now();
    }
}
