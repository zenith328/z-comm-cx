package com.zcommcx.personalization.service;

import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "AI 키워드 제안" 기능이 실제로 어느 세그먼트 고객이 리뷰를 썼는지 알아내기 위한 조회 전담 서비스.
 * Review에 작성 시점 성별/연령 스냅샷(genderAtCreation/ageAtCreation)이 이미 저장돼 있으므로,
 * 그 값으로 바로 세그먼트를 환산한다 — Member를 다시 조회하지 않는다(조회하면 "지금" 값이라
 * 작성 당시와 달라질 수 있음. 예: 그 사이 회원이 연령을 갱신한 경우).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentReviewInsightService {

    // 세그먼트 매칭 대상을 찾기 위해 최신 리뷰를 얼마나 훑어볼지. 데모/중소 규모 트래픽 기준이며,
    // 리뷰 양이 많아지면 세그먼트별 집계 테이블 등으로 개선이 필요하다.
    private static final int SCAN_LIMIT = 300;

    private final ReviewRepository reviewRepository;

    /** 지정한 세그먼트 고객이 쓴 것으로 확인된 리뷰 본문을, 최신순으로 최대 limit개 모아 반환한다. */
    public List<String> collectReviewExcerpts(CustomerSegment segment, int limit) {
        List<Review> candidates = reviewRepository.findByGenderAtCreationIsNotNullAndVisibleTrueOrderByCreatedAtDesc(
                PageRequest.of(0, SCAN_LIMIT));

        return candidates.stream()
                .filter(review -> CustomerSegment.forGenderAndAge(review.getGenderAtCreation(), review.getAgeAtCreation())
                        .map(reviewSegment -> reviewSegment == segment)
                        .orElse(false))
                .map(Review::getContent)
                .limit(limit)
                .toList();
    }
}
