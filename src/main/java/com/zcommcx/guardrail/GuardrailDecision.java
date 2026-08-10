package com.zcommcx.guardrail;

public record GuardrailDecision(boolean blocked, String reason) {

    public static GuardrailDecision pass() {
        return new GuardrailDecision(false, null);
    }

    public static GuardrailDecision block(String reason) {
        return new GuardrailDecision(true, reason);
    }
}
