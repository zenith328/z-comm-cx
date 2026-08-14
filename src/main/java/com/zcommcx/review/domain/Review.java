package com.zcommcx.review.domain;

import com.zcommcx.member.domain.Gender;
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

    /**
     * 로그인 회원(member 테이블, name+phone 복합키)의 전화번호. NATIVE(앱에서 직접 작성) 리뷰만
     * 로그인 세션에서 채워지고, EXTERNAL(외부 쇼핑몰에서 가져온) 리뷰나 이 필드 도입 이전 레거시
     * 리뷰는 null이다.
     */
    private String memberPhone;

    /**
     * 작성 당시 회원의 성별/연령 스냅샷. "지금 이 회원이 어느 세그먼트인지"를 매번 다시 조회하지
     * 않고 작성 시점 값을 고정해서 저장한다 — 그렇지 않으면 회원이 나중에 연령을 바꿨을 때 예전
     * 리뷰가 함께 다른 세그먼트로 옮겨가버려, "이 세그먼트 고객이 실제로 쓴 리뷰"라는 전제가
     * 깨진다(예: 작성 당시 20대였던 리뷰가, 회원이 나이를 30대로 갱신하는 순간 30대 리뷰로 둔갑).
     * 프로필 미입력 회원이 쓴 리뷰나 EXTERNAL 리뷰는 둘 다 null이다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender genderAtCreation;

    private Integer ageAtCreation;

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
            String productCode, String memberId, String memberPhone, Gender genderAtCreation, Integer ageAtCreation,
            String content, int rating, boolean hasPhoto, ReviewOrigin origin) {
        this.productCode = productCode;
        this.memberId = memberId;
        this.memberPhone = memberPhone;
        this.genderAtCreation = genderAtCreation;
        this.ageAtCreation = ageAtCreation;
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
