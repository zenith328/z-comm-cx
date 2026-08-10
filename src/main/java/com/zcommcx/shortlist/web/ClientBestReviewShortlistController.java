package com.zcommcx.shortlist.web;

import com.zcommcx.review.domain.Review;
import com.zcommcx.review.service.ReviewService;
import com.zcommcx.review.web.dto.ClientReviewResponse;
import com.zcommcx.shortlist.service.BestReviewShortlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 구매자에게 노출되는 이번 주 베스트 리뷰 숏리스트(상품별). 운영자용
 * {@link BestReviewShortlistController}와 달리 ClientReviewResponse로 감싸 내부 판단
 * 정보를 제외하고, 숏리스트 생성 이후 비공개로 전환된 리뷰는 다시 한 번 visible 여부를
 * 확인해 응답에서 제외한다.
 */
@RestController
@RequestMapping("/api/products/{productCode}/best-review-shortlist")
@RequiredArgsConstructor
public class ClientBestReviewShortlistController {

    private final BestReviewShortlistService shortlistService;
    private final ReviewService reviewService;

    @GetMapping
    public List<ClientBestReviewShortlistEntryResponse> getShortlist(@PathVariable String productCode) {
        String weekLabel = shortlistService.currentWeekLabel();
        return shortlistService.findByWeekAndProductCode(weekLabel, productCode).stream()
                .map(entry -> {
                    Review review = reviewService.findById(entry.getReviewId());
                    if (!review.isVisible()) return null;
                    return new ClientBestReviewShortlistEntryResponse(
                            entry.getWeekLabel(), entry.getRank(), ClientReviewResponse.from(review));
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
