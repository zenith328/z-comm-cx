package com.zcommcx.guardrail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "guardrail")
public record GuardrailProperties(
        @DefaultValue("7") int returnWindowDays) {
}
