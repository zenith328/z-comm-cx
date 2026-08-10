package com.zcommcx.shortlist.service;

import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewRepository;
import com.zcommcx.shortlist.domain.BestReviewShortlistEntry;
import com.zcommcx.shortlist.domain.BestReviewShortlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 상품별 베스트 후보(BEST_CANDIDATE) 리뷰 중 상위 N개를 주 단위 숏리스트로 선정한다.
 * 실제 운영에서는 {@link BestReviewShortlistScheduler}가 매주 자동 실행하지만,
 * 관리자가 즉시 재생성하고 싶을 때를 위해 수동 트리거 API도 함께 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BestReviewShortlistService {

    private static final int TOP_N_PER_PRODUCT = 3;

    private final ReviewRepository reviewRepository;
    private final BestReviewShortlistRepository shortlistRepository;

    @Transactional
    public List<BestReviewShortlistEntry> generateForCurrentWeek() {
        String weekLabel = currentWeekLabel();
        shortlistRepository.deleteByWeekLabel(weekLabel);

        List<Review> candidates = reviewRepository.findByClassificationAndVisibleTrue(ReviewClassification.BEST_CANDIDATE);
        Map<String, List<Review>> byProduct = candidates.stream()
                .collect(Collectors.groupingBy(Review::getProductCode));

        List<BestReviewShortlistEntry> entries = byProduct.entrySet().stream()
                .flatMap(entry -> rankTopReviews(entry.getKey(), entry.getValue(), weekLabel).stream())
                .toList();

        List<BestReviewShortlistEntry> saved = shortlistRepository.saveAll(entries);
        log.info("베스트 리뷰 숏리스트 생성 완료. weekLabel={}, 상품 수={}, 총 항목 수={}",
                weekLabel, byProduct.size(), saved.size());
        return saved;
    }

    public List<BestReviewShortlistEntry> findByWeek(String weekLabel) {
        return shortlistRepository.findByWeekLabelOrderByProductCodeAscRankAsc(weekLabel);
    }

    public List<BestReviewShortlistEntry> findByWeekAndProductCode(String weekLabel, String productCode) {
        return shortlistRepository.findByWeekLabelAndProductCodeOrderByRankAsc(weekLabel, productCode);
    }

    public String currentWeekLabel() {
        return toWeekLabel(LocalDate.now());
    }

    private List<BestReviewShortlistEntry> rankTopReviews(String productCode, List<Review> reviews, String weekLabel) {
        List<Review> sorted = reviews.stream()
                .sorted(Comparator.comparing(Review::getRating).reversed()
                        .thenComparing(Comparator.comparing(Review::getCreatedAt).reversed()))
                .limit(TOP_N_PER_PRODUCT)
                .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> new BestReviewShortlistEntry(weekLabel, productCode, sorted.get(i).getId(), i + 1))
                .toList();
    }

    private String toWeekLabel(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int week = date.get(weekFields.weekOfWeekBasedYear());
        int weekBasedYear = date.get(weekFields.weekBasedYear());
        return "%d-W%02d".formatted(weekBasedYear, week);
    }
}
