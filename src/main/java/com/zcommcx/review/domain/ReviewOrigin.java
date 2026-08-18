package com.zcommcx.review.domain;

public enum ReviewOrigin {
    NATIVE,
    EXTERNAL,
    /** AI가 핏 가이드 프롬프트 튜닝/데모 시연용으로 생성한 가상 리뷰. 실제 고객 데이터가 아니다. */
    SYNTHETIC
}
