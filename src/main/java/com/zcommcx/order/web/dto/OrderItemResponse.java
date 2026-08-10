package com.zcommcx.order.web.dto;

import com.zcommcx.order.domain.OrderItem;

public record OrderItemResponse(Long productId, String productName, Long unitPrice, Integer quantity) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(), item.getProductNameSnapshot(), item.getUnitPrice(), item.getQuantity());
    }
}
