package com.zcommcx.review.web;

import com.zcommcx.common.web.dto.PageResponse;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewSentiment;
import com.zcommcx.review.domain.ReviewSortOption;
import com.zcommcx.review.service.FitProfileService;
import com.zcommcx.review.service.ReviewService;
import com.zcommcx.review.web.dto.ClientReviewResponse;
import com.zcommcx.review.web.dto.FitProfileResponse;
import com.zcommcx.review.web.dto.ReviewSummaryRequest;
import com.zcommcx.review.web.dto.ReviewSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구매자(Client)에게 노출되는 리뷰 조회/요약 API. 운영자용 {@link ReviewController}와 분리해
 * riskScore/aiReason 등 내부 판단 정보가 구매자에게 노출되지 않도록 한다.
 */
@RestController
@RequestMapping("/api/products/{productCode}/reviews")
@RequiredArgsConstructor
public class ClientReviewController {

    private final ReviewService reviewService;
    private final FitProfileService fitProfileService;

    @GetMapping
    public PageResponse<ClientReviewResponse> getVisibleReviews(
            @PathVariable String productCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean hasPhoto,
            @RequestParam(required = false) ReviewClassification classification,
            @RequestParam(required = false) ReviewSentiment sentiment,
            @RequestParam(defaultValue = "LATEST") ReviewSortOption sort) {
        Page<Review> reviews = reviewService.findVisiblePage(
                productCode, page, size, hasPhoto, classification, sentiment, sort);
        return PageResponse.from(reviews, ClientReviewResponse::from);
    }

    @PostMapping("/summary")
    public ReviewSummaryResponse summarizeReviews(
            @PathVariable String productCode, @Valid @RequestBody ReviewSummaryRequest request) {
        return ReviewSummaryResponse.from(reviewService.summarizeVisibleReviews(productCode, request.query()));
    }

    @GetMapping("/fit-profile")
    public FitProfileResponse getFitProfile(@PathVariable String productCode) {
        return FitProfileResponse.from(fitProfileService.getOrGenerate(productCode));
    }
}
