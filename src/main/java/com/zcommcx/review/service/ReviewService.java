package com.zcommcx.review.service;

import com.zcommcx.member.domain.Member;
import com.zcommcx.member.domain.MemberRepository;
import com.zcommcx.review.ai.GeneratedSyntheticReview;
import com.zcommcx.review.ai.ReviewSummarizer;
import com.zcommcx.review.ai.ReviewSummaryResult;
import com.zcommcx.review.ai.SyntheticReviewGenerator;
import com.zcommcx.review.domain.ProductFitProfileRepository;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewOrigin;
import com.zcommcx.review.domain.ReviewRepository;
import com.zcommcx.review.domain.ReviewSentiment;
import com.zcommcx.review.domain.ReviewSortOption;
import com.zcommcx.review.domain.ReviewSpecifications;
import com.zcommcx.review.domain.ReviewStatus;
import com.zcommcx.review.domain.ReviewSummaryCache;
import com.zcommcx.review.domain.ReviewSummaryCacheRepository;
import com.zcommcx.review.event.ReviewCreatedEvent;
import com.zcommcx.review.web.dto.ReviewCreateRequest;
import com.zcommcx.review.web.dto.ReviewOverrideRequest;
import com.zcommcx.review.web.dto.SyntheticReviewSeedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryCacheRepository reviewSummaryCacheRepository;
    private final ProductFitProfileRepository productFitProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewSummarizer reviewSummarizer;
    private final SyntheticReviewGenerator syntheticReviewGenerator;
    private final MemberRepository memberRepository;

    @Transactional
    public Review createReview(ReviewCreateRequest request) {
        return createReview(request, ReviewOrigin.NATIVE);
    }

    /**
     * 외부 쇼핑몰에서 가져온 리뷰 등록. 작성자 표시 시 마스킹 대상이 되도록 origin을 구분해 둔다.
     */
    @Transactional
    public Review createExternalReview(ReviewCreateRequest request) {
        return createReview(request, ReviewOrigin.EXTERNAL);
    }

    private Review createReview(ReviewCreateRequest request, ReviewOrigin origin) {
        // 세그먼트 판단에 쓸 성별/연령은 "지금 회원 상태"가 아니라 "작성 시점 회원 상태"를
        // 스냅샷으로 고정해야 하므로, 저장 시점에 한 번만 조회해서 Review에 함께 박아 넣는다.
        Optional<Member> member = (request.memberPhone() == null || request.memberPhone().isBlank())
                ? Optional.empty()
                : memberRepository.findByNameAndPhone(request.memberId(), request.memberPhone());

        Review review = new Review(
                request.productCode(),
                request.memberId(),
                request.memberPhone(),
                member.map(Member::getGender).orElse(null),
                member.map(Member::getAge).orElse(null),
                request.content(),
                request.rating(),
                request.hasPhoto(),
                origin);

        reviewRepository.save(review);
        invalidateSummaryCache(review.getProductCode());
        eventPublisher.publishEvent(new ReviewCreatedEvent(review.getId()));
        return review;
    }

    public Page<Review> findAll(
            int page, int size, String productCode, Boolean visible, ReviewClassification classification,
            ReviewStatus status, ReviewOrigin origin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<Review> spec = ReviewSpecifications.filter(productCode, visible, classification, status, origin);
        return reviewRepository.findAll(spec, pageable);
    }

    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Review overrideClassification(Long id, ReviewOverrideRequest request) {
        Review review = findById(id);
        review.applyManualOverride(request.visible(), request.classification(), request.note());
        reviewRepository.save(review);
        invalidateSummaryCache(review.getProductCode());
        return review;
    }

    /**
     * Gemini 호출 실패(FAILED)로 남은 리뷰를 다시 분석 대기 상태로 되돌려 AI 분석 파이프라인을
     * 재시도한다. createReview와 동일하게 이벤트 발행 후 비동기 리스너가 처리한다.
     */
    @Transactional
    public Review reanalyze(Long id) {
        Review review = findById(id);
        if (review.getStatus() != ReviewStatus.FAILED) {
            throw new IllegalStateException(
                    "FAILED 상태의 리뷰만 재분석할 수 있습니다. id=" + id + ", status=" + review.getStatus());
        }
        review.markPendingReanalysis();
        reviewRepository.save(review);
        eventPublisher.publishEvent(new ReviewCreatedEvent(review.getId()));
        return review;
    }

    /**
     * 구매자에게 노출되는 리뷰 목록. 비공개(visible=false) 처리된 리뷰는 제외한다.
     * 분석이 아직 끝나지 않은(PENDING_AI) 리뷰는 fail-open으로 우선 노출한다.
     */
    public List<Review> findVisibleReviews(String productCode) {
        return reviewRepository.findByProductCodeAndVisibleTrueOrderByCreatedAtDesc(productCode);
    }

    /**
     * 구매자 화면의 리뷰 목록 조회(페이징). 정렬은 {@link ReviewSpecifications#visibleFor}가
     * query.orderBy로 직접 지정하므로, 여기서 넘기는 Pageable에는 Sort를 포함하지 않는다
     * (포함하면 Spring Data가 이 orderBy를 덮어쓴다).
     */
    public Page<Review> findVisiblePage(
            String productCode, int page, int size, Boolean hasPhoto, ReviewClassification classification,
            ReviewSentiment sentiment, ReviewSortOption sort) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Review> spec = ReviewSpecifications.visibleFor(productCode, hasPhoto, classification, sentiment, sort);
        return reviewRepository.findAll(spec, pageable);
    }

    public long countVisibleReviews(String productCode) {
        return reviewRepository.countByProductCodeAndVisibleTrue(productCode);
    }

    /**
     * 상품별 리뷰 요약 결과를 캐시한다. 같은 상품에 같은 질문이 다시 들어오면(예: 상세페이지의
     * "장점 요약" 같은 고정 예시 질문) Gemini를 다시 호출하지 않고 DB에 저장된 값을 반환하고,
     * 처음 들어온 질문일 때만 AI를 호출해 결과를 저장해 둔다. 캐시는 리뷰가 추가되거나
     * 공개여부/분류가 바뀌면({@link #invalidateSummaryCache}) 통째로 삭제된다.
     */
    @Transactional
    public ReviewSummaryResult summarizeVisibleReviews(String productCode, String query) {
        return reviewSummaryCacheRepository.findByProductCodeAndQuery(productCode, query)
                .map(cache -> new ReviewSummaryResult(cache.getSummary(), cache.getReviewCount()))
                .orElseGet(() -> summarizeAndCache(productCode, query));
    }

    private ReviewSummaryResult summarizeAndCache(String productCode, String query) {
        List<Review> visibleReviews = findVisibleReviews(productCode);
        ReviewSummaryResult result = reviewSummarizer.summarize(visibleReviews, query);
        try {
            reviewSummaryCacheRepository.save(
                    new ReviewSummaryCache(productCode, query, result.summary(), result.reviewCount()));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 같은 질문이 먼저 캐시를 저장한 경우 — 이번 요청 결과는 그대로 반환한다.
            log.debug("요약 캐시 저장 경합 발생. productCode={}, query={}", productCode, query);
        }
        return result;
    }

    /**
     * 핏 가이드 프롬프트 튜닝/데모 시연 전용 가상 리뷰를 생성해서 저장한다. 별점/작성자만 다를 뿐
     * 저장·AI 분석·캐시 무효화는 실제 리뷰 등록과 동일한 경로(createReview)를 그대로 탄다 —
     * 실제 데이터와 구분 없이 취급하기로 했으므로(SYNTHETIC은 나중을 위한 표시용일 뿐).
     */
    @Transactional
    public List<Review> generateSyntheticReviews(SyntheticReviewSeedRequest request) {
        List<GeneratedSyntheticReview> generated = syntheticReviewGenerator.generate(
                request.productName(), request.brand(), request.category(), request.description(), request.count());

        List<Review> saved = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            GeneratedSyntheticReview review = generated.get(i);
            ReviewCreateRequest createRequest = new ReviewCreateRequest(
                    request.productCode(), "테스트리뷰어" + (i + 1), null, review.content(), review.rating(), false);
            saved.add(createReview(createRequest, ReviewOrigin.SYNTHETIC));
        }
        return saved;
    }

    private void invalidateSummaryCache(String productCode) {
        reviewSummaryCacheRepository.deleteByProductCode(productCode);
        productFitProfileRepository.findById(productCode).ifPresent(productFitProfileRepository::delete);
    }
}
