package com.zcommcx.personalization.service;

import java.util.List;

/**
 * 리뷰가 충분하면(reviewCount >= MIN_REVIEWS_FOR_SUGGESTION) reviewKeywords만 채워지고
 * generalKeywords/promptText는 비어있다. 리뷰가 부족하면 반대로 reviewKeywords는 빈 리스트고
 * generalKeywords(AI 일반 지식 기반)와 promptText(다른 AI 챗봇에 붙여넣어 물어볼 고정 문구)가
 * 채워진다 — 이 둘은 실제 리뷰 데이터에 근거한 게 아니므로 참고용이다.
 */
public record SegmentKeywordSuggestion(
        List<String> reviewKeywords, int reviewCount, List<String> generalKeywords, String promptText) {
}
