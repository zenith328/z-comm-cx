package com.zcommcx.personalization.service;

import java.util.List;

/**
 * reviewCount는 AI 호출 여부와 무관하게, 분석에 사용된 리뷰 건수를 그대로 보여주기 위한 값이다.
 * fallback이 true면 keywords/searchQuery가 리뷰가 아니라 AI의 일반 지식으로 만들어진
 * 참고용이라는 뜻이다(searchQuery는 fallback일 때만 값이 있음).
 */
public record SegmentKeywordSuggestion(List<String> keywords, int reviewCount, boolean fallback, String searchQuery) {
}
