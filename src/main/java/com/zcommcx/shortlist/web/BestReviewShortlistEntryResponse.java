package com.zcommcx.shortlist.web;

import com.zcommcx.review.web.dto.ReviewResponse;
import com.zcommcx.shortlist.domain.BestReviewShortlistEntry;

public record BestReviewShortlistEntryResponse(
        String weekLabel, String productCode, int rank, ReviewResponse review) {

    public static BestReviewShortlistEntryResponse of(BestReviewShortlistEntry entry, ReviewResponse review) {
        return new BestReviewShortlistEntryResponse(entry.getWeekLabel(), entry.getProductCode(), entry.getRank(), review);
    }
}
