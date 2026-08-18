package com.zcommcx.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * productName/brand는 별도로 Product를 조회하지 않고 호출자(어드민 상품상세 화면)가 이미 알고
 * 있는 값을 그대로 실어 보낸다 — review 패키지는 원래 product와 DB FK 없이 productCode
 * 문자열로만 느슨하게 연결되는 관례를 따른 것이다.
 */
public record SyntheticReviewSeedRequest(
        @NotBlank String productCode,
        @NotBlank String productName,
        String brand,
        // 카테고리/설명 둘 다 상품 오판을 막기 위한 근거다. null이면 AI가 상품명만으로 추정해야
        // 하니 되도록 채워서 보내는 게 좋다.
        String category,
        String description,
        @Min(1) @Max(20) int count) {
}
