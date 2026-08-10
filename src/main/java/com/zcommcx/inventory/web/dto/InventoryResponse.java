package com.zcommcx.inventory.web.dto;

import com.zcommcx.inventory.domain.Inventory;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        LocalDateTime updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProduct().getId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity(),
                inventory.getUpdatedAt());
    }
}
