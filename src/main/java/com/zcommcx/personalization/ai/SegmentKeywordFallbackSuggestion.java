package com.zcommcx.personalization.ai;

import java.util.List;

/**
 * 리뷰가 부족해 리뷰 기반 분석이 불가능할 때, AI의 일반 지식으로 대신 제안하는 결과.
 * keywords/searchQuery 모두 실제 리뷰 데이터에 근거한 게 아니라 참고용이다.
 */
public record SegmentKeywordFallbackSuggestion(List<String> keywords, String searchQuery) {
}
