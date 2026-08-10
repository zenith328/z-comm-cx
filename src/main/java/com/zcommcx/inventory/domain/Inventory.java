package com.zcommcx.inventory.domain;

import com.zcommcx.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cs_inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Inventory(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
        this.reservedQuantity = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void adjustQuantity(int delta) {
        this.quantity += delta;
        this.updatedAt = LocalDateTime.now();
    }

    public void reserve(int amount) {
        this.reservedQuantity += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void release(int amount) {
        this.reservedQuantity -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void deduct(int amount) {
        this.quantity -= amount;
        this.reservedQuantity -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문가능수량(quantity - reservedQuantity)을 0으로 만든다.
     * 이미 예약된 수량(reservedQuantity)은 그대로 채워야 할 물량이므로 quantity를 그만큼만 남긴다.
     */
    public void clearStock() {
        this.quantity = this.reservedQuantity;
        this.updatedAt = LocalDateTime.now();
    }
}
