package com.zcommcx.avatar.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AvatarSettingRequest(@NotBlank String imageUrl) {
}
