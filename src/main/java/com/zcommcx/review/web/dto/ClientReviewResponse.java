package com.zcommcx.review.web.dto;

import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewSentiment;

import java.time.LocalDateTime;

/**
 * 구매자 노출용 리뷰 DTO. riskScore/aiReason/overrideNote 등 운영 내부 정보는 노출하지 않는다.
 * sentiment는 구매자 화면의 리뷰 필터/정렬에 쓰이므로 노출한다.
 */
public record ClientReviewResponse(
        Long id,
        String memberId,
        int rating,
        boolean hasPhoto,
        String content,
        LocalDateTime createdAt,
        ReviewClassification classification,
        ReviewSentiment sentiment) {

    public static ClientReviewResponse from(Review review) {
        return new ClientReviewResponse(
                review.getId(),
                review.getMaskedMemberId(),
                review.getRating(),
                review.isHasPhoto(),
                review.getContent(),
                review.getCreatedAt(),
                review.getClassification(),
                review.getSentiment());
    }
}
