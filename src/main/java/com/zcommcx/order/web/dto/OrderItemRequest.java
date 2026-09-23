package com.zcommcx.order.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @param showhostOfferId 쇼호스트 방송 특가로 주문할 때만 채운다. 할인가는 서버가 이 특가로 계산하며,
 *                        클라이언트가 가격을 보내지는 않는다.
 */
public record OrderItemRequest(@NotNull Long productId, @Positive int quantity, Long showhostOfferId) {

    public OrderItemRequest(Long productId, int quantity) {
        this(productId, quantity, null);
    }
}
