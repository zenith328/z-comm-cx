package com.zcommcx.member.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberLoginRequest(@NotBlank String name, @NotBlank String phone) {
}
