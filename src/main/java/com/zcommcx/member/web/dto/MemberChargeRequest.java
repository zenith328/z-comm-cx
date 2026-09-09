package com.zcommcx.member.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record MemberChargeRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @Positive long amount) {
}
