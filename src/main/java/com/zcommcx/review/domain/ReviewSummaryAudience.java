package com.zcommcx.review.domain;

import com.zcommcx.member.domain.Gender;

/**
 * AI 리뷰 요약을 "누구 기준으로" 만들었는지. 같은 질문이라도 보는 고객의 성별에 따라 의미 있는
 * 장단점(특히 사이즈/핏)이 달라지므로 캐시를 성별로 나눈다. 연령까지 나누면 캐시/AI 호출이 6배로
 * 늘어나는 데 비해, 리뷰가 적은 상품에서는 결과 차이가 거의 없어 성별 2종 + 일반(ALL)만 둔다.
 * 비로그인/성별 미입력 고객, AI 상담 채팅처럼 보는 사람을 특정하지 않는 호출은 ALL이다.
 */
public enum ReviewSummaryAudience {
    ALL,
    MALE,
    FEMALE;

    public static ReviewSummaryAudience from(Gender gender) {
        if (gender == null) {
            return ALL;
        }
        return gender == Gender.MALE ? MALE : FEMALE;
    }
}
