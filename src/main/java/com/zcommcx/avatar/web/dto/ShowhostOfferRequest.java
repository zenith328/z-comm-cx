package com.zcommcx.avatar.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 방송 특가를 받을 FO 로그인 회원(이름+전화번호, 프론트 세션 값). */
public record ShowhostOfferRequest(@NotBlank String customerName, @NotBlank String customerPhone) {
}
