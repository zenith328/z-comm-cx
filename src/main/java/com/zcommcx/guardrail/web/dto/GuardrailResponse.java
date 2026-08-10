package com.zcommcx.guardrail.web.dto;

import com.zcommcx.guardrail.GuardrailProperties;

public record GuardrailResponse(int returnWindowDays) {

    public static GuardrailResponse from(GuardrailProperties properties) {
        return new GuardrailResponse(properties.returnWindowDays());
    }
}
