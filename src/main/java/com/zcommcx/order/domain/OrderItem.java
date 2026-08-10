package com.zcommcx.order.domain;

import com.zcommcx.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cs_order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    public OrderItem(Product product, String productNameSnapshot, Long unitPrice, Integer quantity) {
        this.product = product;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}
