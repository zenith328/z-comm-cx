package com.zcommcx.chat.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(String sessionId, @NotBlank String message, String customerName, String customerPhone) {
}
