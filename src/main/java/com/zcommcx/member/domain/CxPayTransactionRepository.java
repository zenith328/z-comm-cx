package com.zcommcx.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CxPayTransactionRepository extends JpaRepository<CxPayTransaction, Long> {

    List<CxPayTransaction> findByMemberNameAndMemberPhoneOrderByCreatedAtDesc(String memberName, String memberPhone);
}
