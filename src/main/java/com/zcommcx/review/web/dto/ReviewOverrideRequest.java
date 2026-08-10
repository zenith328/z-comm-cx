package com.zcommcx.review.web.dto;

import com.zcommcx.review.domain.ReviewClassification;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewOverrideRequest(
        @NotNull Boolean visible,
        @NotNull ReviewClassification classification,
        @Size(max = 500) String note) {
}
