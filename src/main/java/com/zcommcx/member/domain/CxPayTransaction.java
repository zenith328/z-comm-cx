package com.zcommcx.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CX-Pay 잔액의 충전/사용/환불 이력. Member의 balance는 "현재 잔액"만 들고 있고, 실제 변동
 * 내역(언제 얼마가 왜 바뀌었는지)은 여기 별도로 남긴다. Member을 FK로 참조하지 않고
 * memberName/memberPhone을 그대로 저장하는 이유는 Order가 customerName/customerPhone을
 * 그대로 저장하는 것과 같은 이유 — Member의 PK가 복합키(name+phone)라 다른 도메인들이
 * 전부 이 방식을 따르고 있다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "cx_pay_transaction")
public class CxPayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String memberPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CxPayTransactionType type;

    @Column(nullable = false)
    private long amount;

    /** 이 거래가 반영된 직후의 잔액 스냅샷(감사용). */
    @Column(nullable = false)
    private long balanceAfter;

    /**
     * CHARGE는 보통 null, USE/REFUND는 관련 주문번호(orderNo)를 넣는다 — "이 차감/환불이
     * 어느 주문 때문인지" 추적할 수 있게.
     */
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CxPayTransaction(String memberName, String memberPhone, CxPayTransactionType type,
                             long amount, long balanceAfter, String reason) {
        this.memberName = memberName;
        this.memberPhone = memberPhone;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
