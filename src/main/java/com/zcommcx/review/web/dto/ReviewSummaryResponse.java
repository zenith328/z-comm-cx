package com.zcommcx.review.web.dto;

import com.zcommcx.review.ai.ReviewSummaryResult;

public record ReviewSummaryResponse(String summary, int reviewCount) {

    public static ReviewSummaryResponse from(ReviewSummaryResult result) {
        return new ReviewSummaryResponse(result.summary(), result.reviewCount());
    }
}
