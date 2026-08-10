package com.zcommcx.order.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnRequest(@NotBlank String reason) {
}
