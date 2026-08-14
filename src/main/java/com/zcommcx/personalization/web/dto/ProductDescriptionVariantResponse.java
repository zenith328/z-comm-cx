package com.zcommcx.personalization.web.dto;

import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.ProductDescriptionVariant;

import java.time.LocalDateTime;

public record ProductDescriptionVariantResponse(
        CustomerSegment segment,
        String segmentLabel,
        String content,
        String status,
        LocalDateTime generatedAt,
        LocalDateTime approvedAt,
        Integer fitScore,
        String fitScoreReason) {

    public static ProductDescriptionVariantResponse of(CustomerSegment segment, ProductDescriptionVariant entity) {
        if (entity == null) {
            return new ProductDescriptionVariantResponse(
                    segment, segment.getLabel(), null, "NOT_GENERATED", null, null, null, null);
        }
        return new ProductDescriptionVariantResponse(
                segment,
                segment.getLabel(),
                entity.getContent(),
                entity.getStatus().name(),
                entity.getGeneratedAt(),
                entity.getApprovedAt(),
                entity.getFitScore(),
                entity.getFitScoreReason());
    }
}
