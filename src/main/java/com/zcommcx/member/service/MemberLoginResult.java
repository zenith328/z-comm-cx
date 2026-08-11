package com.zcommcx.member.service;

import com.zcommcx.member.domain.Member;

/**
 * 로그인 처리 결과. {@code firstLogin}이 true면 이번 로그인으로 신규 등록된 회원이라는 뜻이며,
 * FO는 이 값을 보고 성별/연령 입력창을 띄울지 결정한다.
 */
public record MemberLoginResult(Member member, boolean firstLogin) {
}
