package com.zcommcx.avatar.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ShowhostQnaRequest(@NotBlank String question) {
}
