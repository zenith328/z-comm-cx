package com.zcommcx.member.service;

import com.zcommcx.member.domain.CxPayTransaction;
import com.zcommcx.member.domain.CxPayTransactionRepository;
import com.zcommcx.member.domain.CxPayTransactionType;
import com.zcommcx.member.domain.Gender;
import com.zcommcx.member.domain.Member;
import com.zcommcx.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final CxPayTransactionRepository cxPayTransactionRepository;

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

    /** CX-Pay 충전. 마이페이지 화면과 CS채팅 둘 다 이 메서드를 쓴다. */
    @Transactional
    public Member charge(String name, String phone, long amount) {
        Member member = getOrCreate(name, phone);
        member.charge(amount);
        recordTransaction(member, CxPayTransactionType.CHARGE, amount, null);
        return member;
    }

    /** CX-Pay 사용(결제 차감). 주문 생성 시 OrderService가 호출한다. */
    @Transactional
    public Member use(String name, String phone, long amount, String orderNo) {
        Member member = getOrCreate(name, phone);
        member.deduct(amount);
        recordTransaction(member, CxPayTransactionType.USE, amount, orderNo);
        return member;
    }

    /** CX-Pay 환불. 주문 취소 시 OrderService가 호출한다. */
    @Transactional
    public Member refund(String name, String phone, long amount, String orderNo) {
        Member member = getOrCreate(name, phone);
        member.refund(amount);
        recordTransaction(member, CxPayTransactionType.REFUND, amount, orderNo);
        return member;
    }

    public List<CxPayTransaction> getTransactionHistory(String name, String phone) {
        return cxPayTransactionRepository.findByMemberNameAndMemberPhoneOrderByCreatedAtDesc(name, phone);
    }

    /**
     * 운영자 회원관리 화면용 목록 조회. 최근 가입한 회원부터 보여준다.
     * search가 있으면 이름 또는 전화번호에 포함되는 회원만 찾는다.
     */
    public Page<Member> list(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search == null || search.isBlank()) {
            return memberRepository.findAll(pageable);
        }
        String query = search.trim();
        return memberRepository.findByNameContainingOrPhoneContaining(query, query, pageable);
    }

    private Member getOrCreate(String name, String phone) {
        return memberRepository.findByNameAndPhone(name, phone)
                .orElseGet(() -> memberRepository.save(new Member(name, phone, null, null, null, null)));
    }

    private void recordTransaction(Member member, CxPayTransactionType type, long amount, String reason) {
        cxPayTransactionRepository.save(new CxPayTransaction(
                member.getName(), member.getPhone(), type, amount, member.getBalance(), reason));
    }
}
