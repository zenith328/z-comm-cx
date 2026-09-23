package com.zcommcx.avatar.web.dto;

import com.zcommcx.avatar.domain.ShowhostOffer;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 남은 시간은 서버가 초 단위로 계산해서 내려준다 — 프론트가 expiresAt을 직접 비교하면 서버(UTC)와
 * 브라우저 시계/시간대 차이 때문에 타이머가 어긋날 수 있다.
 */
public record ShowhostOfferResponse(
        Long id, int discountRate, LocalDateTime expiresAt, long remainingSeconds, boolean used, boolean expired) {

    public static ShowhostOfferResponse from(ShowhostOffer offer) {
        LocalDateTime now = LocalDateTime.now();
        long remaining = Math.max(0, Duration.between(now, offer.getExpiresAt()).getSeconds());
        return new ShowhostOfferResponse(
                offer.getId(), offer.getDiscountRate(), offer.getExpiresAt(), remaining,
                offer.isUsed(), offer.isExpired(now));
    }
}
