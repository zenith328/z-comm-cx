package com.zcommcx.personalization.service;

import com.zcommcx.member.domain.Member;
import com.zcommcx.member.domain.MemberRepository;
import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "AI 키워드 제안" 기능이 실제로 어느 세그먼트 고객이 리뷰를 썼는지 알아내기 위한 조회 전담 서비스.
 * Review에는 (memberId=이름, memberPhone=전화번호)만 문자열로 남아있고 Member와 FK 관계가 없으므로,
 * 리뷰마다 Member를 조회해 성별+연령 → 세그먼트로 환산하는 조인을 애플리케이션 레벨에서 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentReviewInsightService {

    // 세그먼트 매칭 대상을 찾기 위해 최신 리뷰를 얼마나 훑어볼지. 데모/중소 규모 트래픽 기준이며,
    // 리뷰 양이 많아지면 세그먼트별 집계 테이블 등으로 개선이 필요하다.
    private static final int SCAN_LIMIT = 300;

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    /** 지정한 세그먼트 고객이 쓴 것으로 확인된 리뷰 본문을, 최신순으로 최대 limit개 모아 반환한다. */
    public List<String> collectReviewExcerpts(CustomerSegment segment, int limit) {
        List<Review> candidates = reviewRepository.findByMemberPhoneIsNotNullAndVisibleTrueOrderByCreatedAtDesc(
                PageRequest.of(0, SCAN_LIMIT));

        // 동일 회원이 여러 리뷰를 썼을 수 있으니 Member 조회 결과를 재사용한다.
        Map<String, Optional<Member>> memberCache = new HashMap<>();

        return candidates.stream()
                .filter(review -> resolveSegment(review, memberCache)
                        .map(reviewSegment -> reviewSegment == segment)
                        .orElse(false))
                .map(Review::getContent)
                .limit(limit)
                .toList();
    }

    private Optional<CustomerSegment> resolveSegment(Review review, Map<String, Optional<Member>> memberCache) {
        String cacheKey = review.getMemberId() + "|" + review.getMemberPhone();
        Optional<Member> member = memberCache.computeIfAbsent(
                cacheKey, key -> memberRepository.findByNameAndPhone(review.getMemberId(), review.getMemberPhone()));
        return member.flatMap(m -> CustomerSegment.forGenderAndAge(m.getGender(), m.getAge()));
    }
}
