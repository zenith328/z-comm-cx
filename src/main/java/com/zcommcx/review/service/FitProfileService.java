package com.zcommcx.review.service;

import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import com.zcommcx.review.ai.FitAxisLabels;
import com.zcommcx.review.ai.FitProfileGenerator;
import com.zcommcx.review.ai.FitProfileResult;
import com.zcommcx.review.domain.ProductFitProfile;
import com.zcommcx.review.domain.ProductFitProfileRepository;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "AI 리뷰 요약봇"과 별개로, 상품별 핏(사이즈감) 정보를 구조화해서 보여주는 핏 가이드 카드.
 * 실제 (공개) 리뷰가 충분하면 리뷰 내용을 근거로, 부족하면(Cold Start) 상품 설명만 근거로
 * 조심스럽게 추정한다. 결과는 상품당 1건으로 캐시되며, 리뷰가 바뀌면 무효화된다
 * ({@link ReviewService}, {@link com.zcommcx.review.event.ReviewAiAnalysisListener}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FitProfileService {

    // 이보다 적은 리뷰로는 핏 판단 근거가 부족하다고 보고 Cold Start(상품 설명 기반) 폴백으로 전환한다.
    private static final int MIN_REVIEWS_FOR_FIT_PROFILE = 10;

    private final ProductFitProfileRepository repository;
    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final FitProfileGenerator fitProfileGenerator;

    // 클래스 레벨의 readOnly=true를 이 메서드에서만 덮어쓴다 — 캐시 미스 시 save()까지 한 트랜잭션
    // 안에서 처리해야 하는데, private 메서드를 별도로 @Transactional 붙여봐야 self-invocation이라
    // 프록시를 안 타서 무시된다(클래스 레벨 readOnly가 그대로 적용돼 save()가 실패함).
    @Transactional
    public ProductFitProfile getOrGenerate(String productCode) {
        return repository.findById(productCode).orElseGet(() -> generateAndCache(productCode));
    }

    private ProductFitProfile generateAndCache(String productCode) {
        Product product = productService.getByProductCode(productCode);
        List<Review> reviews = reviewRepository.findByProductCodeAndVisibleTrueOrderByCreatedAtDesc(productCode);

        // 카테고리에 맞는 축 이름을 먼저 정해서, AI에게도 "무엇에 대한 핏인지" 알려주고
        // 화면에도 그대로 보여준다 — 신발인데 "어깨" 핏을 묻는 것처럼 어색해지지 않도록.
        FitAxisLabels.Labels labels = FitAxisLabels.forCategory(product.getCategory());

        boolean coldStart = reviews.size() < MIN_REVIEWS_FOR_FIT_PROFILE;
        FitProfileResult result = coldStart
                ? fitProfileGenerator.generateFromDescription(
                        product.getName(), product.getCategory(), product.getDescription(), labels)
                : fitProfileGenerator.generateFromReviews(reviews, labels);

        ProductFitProfile profile = new ProductFitProfile(
                productCode,
                labels.axis1(),
                labels.axis2(),
                labels.axis3(),
                result.shoulderFit(),
                result.chestFit(),
                result.lengthFit(),
                result.recommendedBodyType(),
                result.summary(),
                reviews.size(),
                coldStart);
        return repository.save(profile);
    }
}
