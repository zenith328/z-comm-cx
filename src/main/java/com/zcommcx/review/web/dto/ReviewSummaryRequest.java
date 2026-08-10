package com.zcommcx.review.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewSummaryRequest(@NotBlank @Size(max = 200) String query) {
}
