package com.zcommcx.review.ai;

import com.zcommcx.review.domain.FitLevel;

public record FitProfileResult(
        FitLevel shoulderFit,
        FitLevel chestFit,
        FitLevel lengthFit,
        String recommendedBodyType,
        String summary) {
}
