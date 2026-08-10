package com.zcommcx.order.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(@NotBlank String reason) {
}
