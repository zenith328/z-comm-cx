package com.zcommcx.review.ai;

import java.util.List;

public interface SyntheticReviewGenerator {

    /**
     * 핏 가이드 프롬프트 튜닝/데모 시연 전용 가상 리뷰를 생성한다. 실제 고객 데이터가 아니므로
     * 반드시 {@link com.zcommcx.review.domain.ReviewOrigin#SYNTHETIC}으로만 저장해야 한다.
     * category/description을 함께 넘겨야 한다 — 상품명만으로는 카테고리(신발/의류 등)를 오판해
     * 엉뚱한 핏 표현(예: 신발인데 "어깨가 낙낙하다")이 생성될 수 있다.
     */
    List<GeneratedSyntheticReview> generate(String productName, String brand, String category, String description, int count);
}
