package com.zcommcx.personalization.web.dto;

import com.zcommcx.personalization.domain.SegmentKeywordHistory;

import java.time.LocalDateTime;

public record SegmentKeywordHistoryResponse(String keywords, LocalDateTime changedAt) {

    public static SegmentKeywordHistoryResponse of(SegmentKeywordHistory entity) {
        return new SegmentKeywordHistoryResponse(entity.getKeywords(), entity.getChangedAt());
    }
}
