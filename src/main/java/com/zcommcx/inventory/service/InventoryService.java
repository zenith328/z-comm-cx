package com.zcommcx.inventory.service;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.inventory.domain.Inventory;
import com.zcommcx.inventory.domain.InventoryRepository;
import com.zcommcx.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public Inventory createForProduct(Product product, int initialQuantity) {
        Inventory inventory = new Inventory(product, initialQuantity);
        return inventoryRepository.save(inventory);
    }

    public Inventory getByProductId(Long productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new NotFoundException("상품(id=%d)의 재고 정보가 없습니다.".formatted(productId)));
    }

    /**
     * 상품 목록 조회처럼 재고가 없을 수도 있는 상황(레거시 데이터 등)에서 예외 없이 조회한다.
     */
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoryRepository.findByProduct_Id(productId);
    }

    @Transactional
    public Inventory restock(Long productId, int quantity) {
        Inventory inventory = getByProductId(productId);
        inventory.adjustQuantity(quantity);
        return inventory;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inventory = getByProductId(productId);
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            throw new IllegalStateException(
                    "재고가 부족합니다. (상품 id=%d, 가용재고=%d, 요청수량=%d)".formatted(productId, available, quantity));
        }
        inventory.reserve(quantity);
    }

    @Transactional
    public void release(Long productId, int quantity) {
        Inventory inventory = getByProductId(productId);
        inventory.release(quantity);
    }

    @Transactional
    public void deduct(Long productId, int quantity) {
        Inventory inventory = getByProductId(productId);
        inventory.deduct(quantity);
    }

    @Transactional
    public Inventory markOutOfStock(Long productId) {
        Inventory inventory = getByProductId(productId);
        inventory.clearStock();
        return inventory;
    }
}
