package com.zcommcx.personalization.service;

/**
 * FO에 실제로 보여줄 상세설명. {@code personalized}가 true면 고객의 성별/연령 세그먼트에 맞춰
 * 승인된 설명을 골라 보여준 것이고, false면 세그먼트 매칭이 안 돼(성별/연령 미입력, 미승인 등)
 * 기본 상세설명으로 대체된 것이다.
 */
public record ResolvedDescription(String text, boolean personalized) {
}
