package com.zcommcx.review.ai;

import com.zcommcx.review.domain.Review;

import java.util.List;

public interface FitProfileGenerator {

    /**
     * 리뷰 목록에 언급된 사이즈/핏 정보를 근거로 핏 프로필을 판정한다(reviews는 비어있지 않아야 함).
     * labels는 카테고리에 맞춰 미리 정한 3개 축 이름(예: 신발이면 발볼/사이즈/발등)이다.
     */
    FitProfileResult generateFromReviews(List<Review> reviews, FitAxisLabels.Labels labels);

    /**
     * 리뷰가 부족한 상품(Cold Start)에 대해, 상품명/카테고리/상세설명만 근거로 핏 프로필을
     * 조심스럽게 추정한다. 근거가 없으면 UNKNOWN/정보 부족으로 채워진다.
     */
    FitProfileResult generateFromDescription(String productName, String category, String description, FitAxisLabels.Labels labels);
}
