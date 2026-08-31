package com.zcommcx.personalization.service;

import com.zcommcx.personalization.ai.SegmentKeywordSuggester;
import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.SegmentKeyword;
import com.zcommcx.personalization.domain.SegmentKeywordHistory;
import com.zcommcx.personalization.domain.SegmentKeywordHistoryRepository;
import com.zcommcx.personalization.domain.SegmentKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentKeywordService {

    // 이보다 적은 리뷰로는 AI에게 의미 있는 패턴을 기대하기 어려워 호출 자체를 생략한다(토큰 절약).
    private static final int MIN_REVIEWS_FOR_SUGGESTION = 3;
    private static final int MAX_REVIEW_EXCERPTS = 30;

    private final SegmentKeywordRepository repository;
    private final SegmentKeywordHistoryRepository historyRepository;
    private final SegmentReviewInsightService reviewInsightService;
    private final SegmentKeywordSuggester keywordSuggester;

    public List<SegmentKeyword> listAll() {
        return repository.findAll();
    }

    /**
     * 현재 값(segment_keyword)을 갱신하는 것과 별개로, 저장 시점의 스냅샷을 이력 테이블에도
     * 남긴다 — "언제 어떤 키워드였는지"를 나중에 세그먼트별로 조회하기 위함이다.
     */
    @Transactional
    public SegmentKeyword upsert(CustomerSegment segment, String keywords) {
        SegmentKeyword saved = repository.findById(segment)
                .map(existing -> {
                    existing.updateKeywords(keywords);
                    return existing;
                })
                .orElseGet(() -> repository.save(new SegmentKeyword(segment, keywords)));
        historyRepository.save(new SegmentKeywordHistory(segment, keywords));
        return saved;
    }

    public String getKeywords(CustomerSegment segment) {
        return repository.findById(segment).map(SegmentKeyword::getKeywords).orElse(null);
    }

    /** 이 세그먼트의 키워드 변경 이력을 최신순으로 페이지 단위 조회한다. */
    public Page<SegmentKeywordHistory> getHistory(CustomerSegment segment, int page, int size) {
        return historyRepository.findBySegmentOrderByChangedAtDesc(segment, PageRequest.of(page, size));
    }

    /**
     * 이 세그먼트 고객이 쓴 것으로 확인된 리뷰를 모아 AI에게 키워드 후보를 제안받는다. 리뷰가
     * 너무 적으면(MIN_REVIEWS_FOR_SUGGESTION 미만) AI를 호출하지 않고 빈 결과를 반환한다 —
     * 호출자(컨트롤러/화면)는 reviewCount로 "리뷰 부족" 상황과 "리뷰는 있는데 제안 없음"을 구분한다.
     */
    public SegmentKeywordSuggestion suggestKeywords(CustomerSegment segment) {
        List<String> excerpts = reviewInsightService.collectReviewExcerpts(segment, MAX_REVIEW_EXCERPTS);
        if (excerpts.size() < MIN_REVIEWS_FOR_SUGGESTION) {
            return new SegmentKeywordSuggestion(List.of(), excerpts.size());
        }

        List<String> suggested = keywordSuggester.suggest(segment, excerpts, getKeywords(segment));
        return new SegmentKeywordSuggestion(suggested, excerpts.size());
    }
}
