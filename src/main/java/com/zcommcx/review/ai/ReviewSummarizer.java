package com.zcommcx.review.ai;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.review.domain.Review;

import java.util.List;

public interface ReviewSummarizer {

    /**
     * @param viewerGender 요약을 보는 고객의 성별. null이면 특정 고객을 가정하지 않은 일반 요약을 만든다.
     */
    ReviewSummaryResult summarize(List<Review> reviews, String query, Gender viewerGender);
}
