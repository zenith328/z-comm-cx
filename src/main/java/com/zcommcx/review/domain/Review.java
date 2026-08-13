package com.zcommcx.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private boolean hasPhoto;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(nullable = false)
    private boolean visible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewClassificationSource classificationSource;

    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private ReviewSentiment sentiment;

    @Column(columnDefinition = "text")
    private String aiReason;

    @Column(columnDefinition = "text")
    private String overrideNote;

    private LocalDateTime overriddenAt;

    @Enumerated(EnumType.STRING)
    private ReviewOrigin origin;

    public Review(
            String productCode, String memberId, String content, int rating, boolean hasPhoto,
            ReviewOrigin origin) {
        this.productCode = productCode;
        this.memberId = memberId;
        this.content = content;
        this.rating = rating;
        this.hasPhoto = hasPhoto;
        this.origin = origin;
        this.createdAt = LocalDateTime.now();
        this.status = ReviewStatus.PENDING_AI;
        this.visible = true;
        this.classification = ReviewClassification.NONE;
        this.classificationSource = ReviewClassificationSource.AI;
    }

    public void applyAiResult(
            boolean visible,
            ReviewClassification classification,
            ReviewSentiment sentiment,
            int riskScore,
            String reason) {
        this.visible = visible;
        this.classification = classification;
        this.classificationSource = ReviewClassificationSource.AI;
        this.sentiment = sentiment;
        this.riskScore = riskScore;
        this.aiReason = reason;
        this.status = ReviewStatus.ANALYZED;
    }

    public void markAnalysisFailed(String reason) {
        this.status = ReviewStatus.FAILED;
        this.aiReason = reason;
    }

    public void markPendingReanalysis() {
        this.status = ReviewStatus.PENDING_AI;
    }

    public void applyManualOverride(boolean visible, ReviewClassification classification, String note) {
        this.visible = visible;
        this.classification = classification;
        this.classificationSource = ReviewClassificationSource.ADMIN;
        this.overrideNote = note;
        this.overriddenAt = LocalDateTime.now();
        this.status = ReviewStatus.ANALYZED;
    }

    /**
     * 외부에서 가져온 리뷰(origin=EXTERNAL, 또는 이 필드 도입 이전에 등록되어 origin이 없는
     * 레거시 리뷰)는 작성자 첫 글자만 남기고 나머지를 마스킹한다. 앱에서 직접 작성한(NATIVE)
     * 리뷰는 회원이 스스로 입력한 식별자이므로 마스킹하지 않는다.
     */
    public String getMaskedMemberId() {
        if (origin == ReviewOrigin.NATIVE) {
            return memberId;
        }
        if (memberId == null || memberId.length() <= 1) {
            return memberId;
        }
        return memberId.charAt(0) + "*".repeat(memberId.length() - 1);
    }
}
