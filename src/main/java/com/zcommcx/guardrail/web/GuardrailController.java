package com.zcommcx.guardrail.web;

import com.zcommcx.guardrail.GuardrailProperties;
import com.zcommcx.guardrail.web.dto.GuardrailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardrail")
@RequiredArgsConstructor
public class GuardrailController {

    private final GuardrailProperties guardrailProperties;

    @GetMapping
    public GuardrailResponse get() {
        return GuardrailResponse.from(guardrailProperties);
    }
}
