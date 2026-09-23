package com.zcommcx.review.web.dto;

import com.zcommcx.member.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param viewerGender 요약을 보는 로그인 회원의 성별(프론트가 세션 값을 그대로 전달). 비로그인이거나
 *                     성별 미입력이면 null — 일반 요약을 받는다.
 */
public record ReviewSummaryRequest(@NotBlank @Size(max = 200) String query, Gender viewerGender) {
}
