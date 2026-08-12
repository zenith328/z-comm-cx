package com.zcommcx.review.event;

import com.zcommcx.common.exception.AiQuotaExceededException;
import com.zcommcx.review.ai.ReviewAiClassificationException;
import com.zcommcx.review.ai.ReviewAiResult;
import com.zcommcx.review.ai.ReviewClassifier;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewRepository;
import com.zcommcx.review.domain.ReviewStatus;
import com.zcommcx.review.domain.ReviewSummaryCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@org.springframework.stereotype.Component
public class ReviewAiAnalysisListener {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryCacheRepository reviewSummaryCacheRepository;
    private final ReviewClassifier reviewClassifier;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        Review review = reviewRepository.findById(event.reviewId()).orElse(null);
        if (review == null) {
            log.warn("AI 분석 대상 리뷰를 찾을 수 없습니다. reviewId={}", event.reviewId());
            return;
        }

        try {
            ReviewAiResult result = reviewClassifier.classify(review);
            applyIfStillPending(event.reviewId(), fresh -> fresh.applyAiResult(
                    result.visible(), result.classification(), result.sentiment(), result.riskScore(), result.reason()));
            // AI가 공개여부/분류를 확정했으므로, 이 상품의 요약 캐시는 더 이상 최신이 아니다.
            reviewSummaryCacheRepository.deleteByProductCode(review.getProductCode());
        } catch (AiQuotaExceededException e) {
            log.error("리뷰 AI 분석이 Gemini 사용량 한도 초과로 실패했습니다. reviewId={}", event.reviewId(), e);
            applyIfStillPending(event.reviewId(),
                    fresh -> fresh.markAnalysisFailed("AI 사용량 한도를 초과해 분석하지 못했습니다. 잠시 후 재시도해주세요."));
        } catch (ReviewAiClassificationException e) {
            log.error("리뷰 AI 분석 실패. reviewId={}", event.reviewId(), e);
            applyIfStillPending(event.reviewId(), fresh -> fresh.markAnalysisFailed("AI 분석 중 오류가 발생했습니다."));
        }
    }

    /**
     * Gemini 호출 도중 관리자가 먼저 수동 override 했을 수 있으므로,
     * 응답을 받은 시점에 상태를 다시 조회해 여전히 PENDING_AI일 때만 반영한다.
     */
    private void applyIfStillPending(Long reviewId, Consumer<Review> apply) {
        Review fresh = reviewRepository.findById(reviewId).orElse(null);
        if (fresh == null) {
            return;
        }
        if (fresh.getStatus() != ReviewStatus.PENDING_AI) {
            log.info("리뷰가 이미 처리되어(status={}) AI 분석 결과를 적용하지 않습니다. reviewId={}", fresh.getStatus(), reviewId);
            return;
        }
        apply.accept(fresh);
        reviewRepository.save(fresh);
    }
}
