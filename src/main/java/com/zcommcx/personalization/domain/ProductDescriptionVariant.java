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

    /**
     * AI가 생성 직후 스스로 매긴 "운영자 키워드/세그먼트 반영도" 점수(0~100)와 근거.
     * 관리자가 수동으로 수정(editManually)하면 더 이상 AI 생성물이 아니므로 비워둔다.
     */
    private Integer fitScore;

    @Column(columnDefinition = "TEXT")
    private String fitScoreReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DescriptionVariantStatus status;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProductDescriptionVariant(
            Product product, CustomerSegment segment, String content, Integer fitScore, String fitScoreReason) {
        this.product = product;
        this.segment = segment;
        applyGenerated(content, fitScore, fitScoreReason);
    }

    /** AI (재)생성 결과 반영 — 콘텐츠와 함께 AI 자체 평가 점수/근거도 갱신한다. */
    public void regenerate(String content, Integer fitScore, String fitScoreReason) {
        applyGenerated(content, fitScore, fitScoreReason);
    }

    /** 관리자가 직접 수정 — AI가 매긴 적합도는 더 이상 이 내용을 반영하지 않으므로 비운다. */
    public void editManually(String content) {
        applyGenerated(content, null, null);
    }

    private void applyGenerated(String content, Integer fitScore, String fitScoreReason) {
        this.content = content;
        this.fitScore = fitScore;
        this.fitScoreReason = fitScoreReason;
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
