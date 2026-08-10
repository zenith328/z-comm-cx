package com.zcommcx.inventory.web.dto;

import jakarta.validation.constraints.Positive;

public record RestockRequest(@Positive int quantity) {
}
