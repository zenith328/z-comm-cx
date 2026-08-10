package com.zcommcx.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품별 AI 리뷰 요약 결과 캐시. 같은 상품에 같은 질문(query)이 다시 들어오면 Gemini를 다시
 * 호출하지 않고 이 값을 반환한다. 리뷰가 추가되거나 공개여부/분류가 바뀌면(AI 자동 판단이든
 * 관리자 수동 override든) 해당 상품의 캐시를 통째로 삭제해 다음 요청에서 다시 계산하게 한다
 * ({@link com.zcommcx.review.service.ReviewService}, {@link com.zcommcx.review.event.ReviewAiAnalysisListener}).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "review_summary_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_code", "query"}))
public class ReviewSummaryCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String query;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReviewSummaryCache(String productCode, String query, String summary, int reviewCount) {
        this.productCode = productCode;
        this.query = query;
        this.summary = summary;
        this.reviewCount = reviewCount;
        this.createdAt = LocalDateTime.now();
    }
}
