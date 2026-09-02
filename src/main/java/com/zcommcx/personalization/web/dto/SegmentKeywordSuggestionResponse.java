package com.zcommcx.personalization.web.dto;

import com.zcommcx.personalization.service.SegmentKeywordSuggestion;

import java.util.List;

public record SegmentKeywordSuggestionResponse(
        List<String> reviewKeywords, int reviewCount, List<String> generalKeywords, String promptText) {

    public static SegmentKeywordSuggestionResponse of(SegmentKeywordSuggestion suggestion) {
        return new SegmentKeywordSuggestionResponse(
                suggestion.reviewKeywords(),
                suggestion.reviewCount(),
                suggestion.generalKeywords(),
                suggestion.promptText());
    }
}
