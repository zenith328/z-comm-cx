package com.zcommcx.personalization.service;

import com.zcommcx.personalization.ai.SegmentKeywordFallbackSuggestion;
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
     * "AI 추천 키워드"를 누르면 항상 세 가지를 함께 제안한다: ①리뷰 기반 키워드(리뷰가
     * MIN_REVIEWS_FOR_SUGGESTION 이상일 때만 AI 호출, 부족하면 호출 자체를 생략하고 빈 리스트),
     * ②AI 일반 지식 기반 키워드, ③관리자가 직접 검색해볼 검색어. ②③은 리뷰 충분 여부와 무관하게
     * 항상 생성한다 — 리뷰가 충분해도 참고용으로 함께 보여준다.
     *
     * <p>클래스 레벨 readOnly 트랜잭션을 여기서는 반드시 오버라이드해야 한다 — keywordSuggester 호출이
     * 내부적으로 GeminiApiUsageService.recordRequest()를 통해 사용량을 DB에 기록(INSERT)하는데,
     * readOnly 트랜잭션 안에서는 이 INSERT가 거부되어(PostgreSQL: "cannot execute INSERT in a
     * read-only transaction") 전체 요청이 UnexpectedRollbackException으로 실패한다.
     */
    @Transactional
    public SegmentKeywordSuggestion suggestKeywords(CustomerSegment segment) {
        List<String> excerpts = reviewInsightService.collectReviewExcerpts(segment, MAX_REVIEW_EXCERPTS);
        String existingKeywords = getKeywords(segment);

        List<String> reviewKeywords = excerpts.size() >= MIN_REVIEWS_FOR_SUGGESTION
                ? keywordSuggester.suggest(segment, excerpts, existingKeywords)
                : List.of();
        SegmentKeywordFallbackSuggestion general = keywordSuggester.suggestFallback(segment, existingKeywords);

        return new SegmentKeywordSuggestion(reviewKeywords, excerpts.size(), general.keywords(), general.searchQuery());
    }
}
