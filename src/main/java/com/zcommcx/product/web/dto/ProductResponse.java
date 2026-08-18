package com.zcommcx.product.web.dto;

import com.zcommcx.product.domain.Product;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String sourceUrl,
        String productCode,
        String name,
        String brand,
        String category,
        Long price,
        String description,
        List<String> imageUrls,
        long reviewCount,
        Integer stockQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        LocalDateTime createdAt) {

    public static ProductResponse from(Product product, long reviewCount, Integer stockQuantity, Integer reservedQuantity) {
        Integer available =
                (stockQuantity != null && reservedQuantity != null) ? stockQuantity - reservedQuantity : null;
        return new ProductResponse(
                product.getId(),
                product.getSourceUrl(),
                product.getProductCode(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrls(),
                reviewCount,
                stockQuantity,
                reservedQuantity,
                available,
                product.getCreatedAt());
    }
}
