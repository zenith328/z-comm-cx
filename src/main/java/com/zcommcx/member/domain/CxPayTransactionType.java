package com.zcommcx.member.domain;

public enum CxPayTransactionType {
    /** 충전. */
    CHARGE,
    /** 주문 결제로 사용(차감). */
    USE,
    /** 주문 취소로 환불. */
    REFUND
}
