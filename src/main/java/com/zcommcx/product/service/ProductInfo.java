package com.zcommcx.product.service;

import java.util.List;

public record ProductInfo(
        String sourceUrl, String productCode, String name, String brand, String category, String description,
        Long price, List<String> imageUrls) {
}
