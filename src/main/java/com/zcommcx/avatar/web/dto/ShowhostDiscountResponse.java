package com.zcommcx.avatar.web.dto;

import com.zcommcx.avatar.domain.ShowhostDiscount;

public record ShowhostDiscountResponse(boolean enabled, int discountRate, int validMinutes) {

    public static ShowhostDiscountResponse from(ShowhostDiscount discount) {
        return new ShowhostDiscountResponse(discount.isEnabled(), discount.getDiscountRate(), discount.getValidMinutes());
    }
}
