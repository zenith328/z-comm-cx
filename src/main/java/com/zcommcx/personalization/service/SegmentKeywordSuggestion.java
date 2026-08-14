package com.zcommcx.personalization.service;

import java.util.List;

/** reviewCount는 AI 호출 여부와 무관하게, 분석에 사용된 리뷰 건수를 그대로 보여주기 위한 값이다. */
public record SegmentKeywordSuggestion(List<String> keywords, int reviewCount) {
}
