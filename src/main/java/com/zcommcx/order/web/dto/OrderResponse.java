package com.zcommcx.order.web.dto;

import com.zcommcx.order.domain.Order;
import com.zcommcx.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        String customerName,
        String customerPhone,
        OrderStatus status,
        String recipientName,
        String recipientPhone,
        String zipcode,
        String address1,
        String address2,
        LocalDateTime orderedAt,
        LocalDateTime shippedAt,
        String statusReason,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getStatus(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getZipcode(),
                order.getAddress1(),
                order.getAddress2(),
                order.getOrderedAt(),
                order.getShippedAt(),
                order.getStatusReason(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
