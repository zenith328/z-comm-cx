package com.zcommcx.member.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@link Member}의 복합키. 정식 회원 로그인이 없는 구조라 이름+전화번호 조합을
 * 식별자로 쓴다(동명이인이라도 전화번호가 다르면 별개 회원으로 취급).
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MemberId implements Serializable {
    private String name;
    private String phone;
}
