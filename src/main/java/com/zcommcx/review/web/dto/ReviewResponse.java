package com.zcommcx.review.web.dto;

import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewClassificationSource;
import com.zcommcx.review.domain.ReviewSentiment;
import com.zcommcx.review.domain.ReviewStatus;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        String productCode,
        String memberId,
        String content,
        int rating,
        boolean hasPhoto,
        LocalDateTime createdAt,
        ReviewStatus status,
        boolean visible,
        ReviewClassification classification,
        ReviewClassificationSource classificationSource,
        ReviewSentiment sentiment,
        Integer riskScore,
        String aiReason,
        String overrideNote,
        LocalDateTime overriddenAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductCode(),
                review.getMaskedMemberId(),
                review.getContent(),
                review.getRating(),
                review.isHasPhoto(),
                review.getCreatedAt(),
                review.getStatus(),
                review.isVisible(),
                review.getClassification(),
                review.getClassificationSource(),
                review.getSentiment(),
                review.getRiskScore(),
                review.getAiReason(),
                review.getOverrideNote(),
                review.getOverriddenAt());
    }
}
