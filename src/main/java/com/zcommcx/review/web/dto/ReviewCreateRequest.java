package com.zcommcx.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewCreateRequest(
        @NotBlank String productCode,
        @NotBlank String memberId,
        @NotBlank String content,
        @Min(1) @Max(5) int rating,
        boolean hasPhoto) {
}
