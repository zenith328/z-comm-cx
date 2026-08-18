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

    /**
     * 상품 카테고리(신발/의류/가방 등). URL 등록 시 원본 사이트의 schema.org category가 있으면
     * 자동으로 채워지고(사이트마다 제공 여부가 다름), 없거나 틀리면 관리자가 직접 수정한다.
     * AI가 합성 리뷰/핏 가이드를 생성할 때 상품명만으로 카테고리를 오판하지 않도록 하는 근거로 쓴다.
     */
    private String category;

    private Long price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_image_url", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Product(ProductInfo info) {
        this.createdAt = LocalDateTime.now();
        // category/description은 최초 등록 시 스크레이핑 결과로만 채우고, updateFrom(재등록/
        // 재스크레이핑)에서는 건드리지 않는다 — 관리자가 나중에 고친 값을 재등록할 때마다
        // 덮어써버리면 안 되기 때문이다.
        this.category = info.category();
        this.description = info.description();
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

    /**
     * 관리자가 직접 입력하는 기본 상품 상세설명. 세그먼트별 AI 설명 생성의 원본 소스로 쓰인다.
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /** 스크레이핑이 카테고리를 못 찾았거나 잘못 판단했을 때 관리자가 직접 고친다. */
    public void updateCategory(String category) {
        this.category = category;
    }
}
