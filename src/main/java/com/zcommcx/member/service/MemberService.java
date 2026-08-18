package com.zcommcx.member.service;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.member.domain.Member;
import com.zcommcx.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 이름+전화번호로 로그인한다. 처음 보는 조합이면 성별/출생년도 없이 새로 등록하고
     * firstLogin=true를 반환한다 — FO는 이 값을 보고 최초 로그인에만 성별/출생년도 입력창을 띄운다.
     */
    @Transactional
    public MemberLoginResult login(String name, String phone) {
        return memberRepository.findByNameAndPhone(name, phone)
                .map(member -> new MemberLoginResult(member, false))
                .orElseGet(() -> new MemberLoginResult(
                        memberRepository.save(new Member(name, phone, null, null, null, null)), true));
    }

    /**
     * 성별/출생년도/체형(키·몸무게)을 갱신한다. 최초 로그인 직후의 추가 입력과, "내 정보"에서의
     * 수정 둘 다 이 메서드를 쓴다.
     */
    @Transactional
    public Member updateProfile(
            String name, String phone, Gender gender, Integer birthYear, Integer heightCm, Integer weightKg) {
        Member member = memberRepository.findByNameAndPhone(name, phone)
                .orElseGet(() -> memberRepository.save(new Member(name, phone, null, null, null, null)));
        member.updateProfile(gender, birthYear, heightCm, weightKg);
        return member;
    }
}
