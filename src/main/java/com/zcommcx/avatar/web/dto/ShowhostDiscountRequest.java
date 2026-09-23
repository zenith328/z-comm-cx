package com.zcommcx.avatar.web.dto;

import jakarta.validation.constraints.NotNull;

public record ShowhostDiscountRequest(@NotNull Boolean enabled, @NotNull Integer discountRate, @NotNull Integer validMinutes) {
}
