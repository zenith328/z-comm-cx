package com.zcommcx.shortlist.web;

import com.zcommcx.review.service.ReviewService;
import com.zcommcx.review.web.dto.ReviewResponse;
import com.zcommcx.shortlist.domain.BestReviewShortlistEntry;
import com.zcommcx.shortlist.service.BestReviewShortlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/best-review-shortlist")
@RequiredArgsConstructor
public class BestReviewShortlistController {

    private final BestReviewShortlistService shortlistService;
    private final ReviewService reviewService;

    @PostMapping("/generate")
    public List<BestReviewShortlistEntryResponse> generate() {
        return toResponses(shortlistService.generateForCurrentWeek());
    }

    @GetMapping
    public List<BestReviewShortlistEntryResponse> getShortlist(
            @RequestParam(required = false) String week) {
        String weekLabel = week != null ? week : shortlistService.currentWeekLabel();
        return toResponses(shortlistService.findByWeek(weekLabel));
    }

    private List<BestReviewShortlistEntryResponse> toResponses(List<BestReviewShortlistEntry> entries) {
        return entries.stream()
                .map(entry -> {
                    ReviewResponse review = ReviewResponse.from(reviewService.findById(entry.getReviewId()));
                    return BestReviewShortlistEntryResponse.of(entry, review);
                })
                .toList();
    }
}
