package com.zcommcx.review.ai;

import com.zcommcx.review.domain.Review;

import java.util.List;

public interface ReviewSummarizer {

    ReviewSummaryResult summarize(List<Review> reviews, String query);
}
