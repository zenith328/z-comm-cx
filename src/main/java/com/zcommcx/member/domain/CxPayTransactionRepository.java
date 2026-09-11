package com.zcommcx.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CxPayTransactionRepository extends JpaRepository<CxPayTransaction, Long> {

    List<CxPayTransaction> findByMemberNameAndMemberPhoneOrderByCreatedAtDesc(String memberName, String memberPhone);

    /** 이 주문이 실제로 CX-Pay로 결제(USE)된 적이 있는지 확인한다. CX-Pay 도입 이전 주문은 결제 이력이 없다. */
    boolean existsByTypeAndReason(CxPayTransactionType type, String reason);
}
