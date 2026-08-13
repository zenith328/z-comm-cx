package com.zcommcx.personalization.domain;

import com.zcommcx.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 세그먼트별 AI 생성 상세설명. 생성 직후엔 항상 DRAFT이며, 관리자가 승인(approve)해야
 * FO 노출 대상(APPROVED)이 된다. 키워드를 바꿔 재생성(regenerate)하면 승인 상태는 초기화된다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "product_description_variant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "segment"}))
public class ProductDescriptionVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerSegment segment;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DescriptionVariantStatus status;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProductDescriptionVariant(Product product, CustomerSegment segment, String content) {
        this.product = product;
        this.segment = segment;
        applyGenerated(content);
    }

    public void regenerate(String content) {
        applyGenerated(content);
    }

    private void applyGenerated(String content) {
        this.content = content;
        this.status = DescriptionVariantStatus.DRAFT;
        this.generatedAt = LocalDateTime.now();
        this.approvedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = DescriptionVariantStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
