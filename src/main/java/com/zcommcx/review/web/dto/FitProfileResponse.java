package com.zcommcx.review.web.dto;

import com.zcommcx.review.domain.FitLevel;
import com.zcommcx.review.domain.ProductFitProfile;

public record FitProfileResponse(
        String axis1Label,
        String axis2Label,
        String axis3Label,
        FitLevel shoulderFit,
        FitLevel chestFit,
        FitLevel lengthFit,
        String recommendedBodyType,
        String summary,
        int basedOnReviewCount,
        boolean fromColdStartFallback) {

    public static FitProfileResponse from(ProductFitProfile profile) {
        return new FitProfileResponse(
                profile.getAxis1Label(),
                profile.getAxis2Label(),
                profile.getAxis3Label(),
                profile.getShoulderFit(),
                profile.getChestFit(),
                profile.getLengthFit(),
                profile.getRecommendedBodyType(),
                profile.getSummary(),
                profile.getBasedOnReviewCount(),
                profile.isFromColdStartFallback());
    }
}
