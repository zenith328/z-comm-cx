package com.zcommcx.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        String zipcode,
        @NotBlank String address1,
        String address2,
        @NotEmpty List<@Valid OrderItemRequest> items) {
}
