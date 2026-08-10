package com.zcommcx.product.domain;

import com.zcommcx.product.service.ProductInfo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceUrl;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    private String brand;

    private Long price;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_image_url", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Product(ProductInfo info) {
        this.createdAt = LocalDateTime.now();
        updateFrom(info);
    }

    public void updateFrom(ProductInfo info) {
        this.sourceUrl = info.sourceUrl();
        this.productCode = info.productCode();
        this.name = info.name();
        this.brand = info.brand();
        this.price = info.price();
        this.imageUrls.clear();
        this.imageUrls.addAll(info.imageUrls());
    }
}
