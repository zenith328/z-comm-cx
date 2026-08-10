package com.zcommcx.product.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductCreateRequest(@NotBlank String url) {
}
