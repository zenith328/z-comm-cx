package com.zcommcx.personalization.web.dto;

import com.zcommcx.personalization.service.SegmentKeywordSuggestion;

import java.util.List;

public record SegmentKeywordSuggestionResponse(
        List<String> keywords, int reviewCount, boolean fallback, String searchQuery) {

    public static SegmentKeywordSuggestionResponse of(SegmentKeywordSuggestion suggestion) {
        return new SegmentKeywordSuggestionResponse(
                suggestion.keywords(), suggestion.reviewCount(), suggestion.fallback(), suggestion.searchQuery());
    }
}
