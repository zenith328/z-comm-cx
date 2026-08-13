package com.zcommcx.member.web.dto;

import com.zcommcx.member.domain.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MemberProfileUpdateRequest(
        @NotBlank String name,
        @NotBlank String phone,
        Gender gender,
        @Min(0) @Max(120) Integer age) {
}
