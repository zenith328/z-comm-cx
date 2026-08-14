package com.zcommcx.personalization.ai;

/**
 * AI가 생성한 세그먼트별 상세설명 + AI 스스로 매긴 적합도 자체평가.
 * fitScore/fitScoreReason은 Gemini 응답 파싱에 실패하면 null일 수 있다(생성 자체는 실패로 보지 않음).
 */
public record GeneratedDescription(String content, Integer fitScore, String fitScoreReason) {
}
