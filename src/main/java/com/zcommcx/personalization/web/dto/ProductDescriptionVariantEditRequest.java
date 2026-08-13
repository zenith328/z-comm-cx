package com.zcommcx.personalization.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductDescriptionVariantEditRequest(@NotBlank String content) {
}
