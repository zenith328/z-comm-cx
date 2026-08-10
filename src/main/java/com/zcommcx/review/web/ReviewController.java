package com.zcommcx.review.web;

import com.zcommcx.common.web.dto.PageResponse;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewStatus;
import com.zcommcx.review.service.ReviewService;
import com.zcommcx.review.web.dto.ReviewCreateRequest;
import com.zcommcx.review.web.dto.ReviewOverrideRequest;
import com.zcommcx.review.web.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = ReviewResponse.from(reviewService.createReview(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<ReviewResponse> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) ReviewClassification classification,
            @RequestParam(required = false) ReviewStatus status) {
        Page<Review> reviews = reviewService.findAll(page, size, productCode, visible, classification, status);
        return PageResponse.from(reviews, ReviewResponse::from);
    }

    @GetMapping("/{id}")
    public ReviewResponse getReview(@PathVariable Long id) {
        return ReviewResponse.from(reviewService.findById(id));
    }

    @PatchMapping("/{id}/classification")
    public ReviewResponse overrideClassification(
            @PathVariable Long id, @Valid @RequestBody ReviewOverrideRequest request) {
        return ReviewResponse.from(reviewService.overrideClassification(id, request));
    }

    @PostMapping("/{id}/reanalyze")
    public ReviewResponse reanalyze(@PathVariable Long id) {
        return ReviewResponse.from(reviewService.reanalyze(id));
    }
}
