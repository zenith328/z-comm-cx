package com.zcommcx.personalization.web.dto;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.SegmentKeyword;

import java.time.LocalDateTime;

public record SegmentKeywordResponse(
        CustomerSegment segment,
        String segmentLabel,
        Gender gender,
        String keywords,
        LocalDateTime updatedAt) {

    public static SegmentKeywordResponse of(CustomerSegment segment, SegmentKeyword entity) {
        return new SegmentKeywordResponse(
                segment,
                segment.getLabel(),
                segment.getGender(),
                entity != null ? entity.getKeywords() : null,
                entity != null ? entity.getUpdatedAt() : null);
    }
}
