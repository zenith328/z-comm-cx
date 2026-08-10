package com.zcommcx.review.ai;

import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewSentiment;

public record ReviewAiResult(
        boolean visible,
        ReviewClassification classification,
        ReviewSentiment sentiment,
        int riskScore,
        String reason) {
}
