package com.zcommcx.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, MemberId> {

    Optional<Member> findByNameAndPhone(String name, String phone);
}
