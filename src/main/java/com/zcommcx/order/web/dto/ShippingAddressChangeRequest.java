package com.zcommcx.order.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressChangeRequest(
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        String zipcode,
        @NotBlank String address1,
        String address2) {
}
