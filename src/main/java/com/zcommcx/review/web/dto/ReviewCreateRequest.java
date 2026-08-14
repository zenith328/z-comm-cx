package com.zcommcx.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewCreateRequest(
        @NotBlank String productCode,
        @NotBlank String memberId,
        // 로그인 세션의 전화번호. 외부 리뷰 가져오기(EXTERNAL)에는 로그인 세션이 없어 없을 수 있어 필수는 아니다.
        String memberPhone,
        @NotBlank String content,
        @Min(1) @Max(5) int rating,
        boolean hasPhoto) {
}
