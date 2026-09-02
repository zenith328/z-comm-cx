package com.zcommcx.personalization.service;

import java.util.List;

/**
 * "AI 추천 키워드"를 누르면 항상 세 가지를 함께 보여준다 — 리뷰 기반 추천은 리뷰가 충분할 때만
 * 의미가 있으므로, reviewKeywords는 reviewCount가 부족하면 항상 빈 리스트다(이때는 AI를 호출하지
 * 않아 리뷰 부족 상황에서 불필요한 비용이 들지 않는다). generalKeywords/searchQuery는 리뷰
 * 충분 여부와 무관하게 AI의 일반 지식으로 항상 생성된다.
 */
public record SegmentKeywordSuggestion(
        List<String> reviewKeywords, int reviewCount, List<String> generalKeywords, String searchQuery) {
}
