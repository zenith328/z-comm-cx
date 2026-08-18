package com.zcommcx.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품별 AI 핏 가이드 결과 캐시(상품당 1건). 리뷰가 추가되거나 공개여부/분류가 바뀌면 리뷰 요약
 * 캐시와 마찬가지로 삭제되어 다음 조회 때 다시 생성된다({@link com.zcommcx.review.service.ReviewService},
 * {@link com.zcommcx.review.event.ReviewAiAnalysisListener}).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "product_fit_profile")
public class ProductFitProfile {

    @Id
    @Column(name = "product_code", nullable = false)
    private String productCode;

    /**
     * shoulderFit/chestFit/lengthFit 3개 슬롯의 이름은 고정이지만, 실제로 "무엇에 대한" 판정인지는
     * 카테고리에 따라 다르다(예: 신발이면 발볼/사이즈/발등). axis1Label~axis3Label이 화면에 보여줄
     * 실제 라벨이고, shoulderFit~lengthFit은 그 라벨에 해당하는 판정값을 담는 슬롯일 뿐이다.
     */
    @Column(name = "axis1_label", nullable = false)
    private String axis1Label;

    @Column(name = "axis2_label", nullable = false)
    private String axis2Label;

    @Column(name = "axis3_label", nullable = false)
    private String axis3Label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FitLevel shoulderFit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FitLevel chestFit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FitLevel lengthFit;

    @Column(nullable = false, columnDefinition = "text")
    private String recommendedBodyType;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "based_on_review_count", nullable = false)
    private int basedOnReviewCount;

    /** true면 리뷰가 부족해(Cold Start) 리뷰가 아니라 상품 설명 기반으로 추정한 결과다. */
    @Column(name = "from_cold_start_fallback", nullable = false)
    private boolean fromColdStartFallback;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public ProductFitProfile(
            String productCode, String axis1Label, String axis2Label, String axis3Label,
            FitLevel shoulderFit, FitLevel chestFit, FitLevel lengthFit,
            String recommendedBodyType, String summary, int basedOnReviewCount, boolean fromColdStartFallback) {
        this.productCode = productCode;
        this.axis1Label = axis1Label;
        this.axis2Label = axis2Label;
        this.axis3Label = axis3Label;
        this.shoulderFit = shoulderFit;
        this.chestFit = chestFit;
        this.lengthFit = lengthFit;
        this.recommendedBodyType = recommendedBodyType;
        this.summary = summary;
        this.basedOnReviewCount = basedOnReviewCount;
        this.fromColdStartFallback = fromColdStartFallback;
        this.generatedAt = LocalDateTime.now();
    }
}
