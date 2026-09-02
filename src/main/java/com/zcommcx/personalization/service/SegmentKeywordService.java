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
     * 리뷰가 충분하면(MIN_REVIEWS_FOR_SUGGESTION 이상) 리뷰 기반 키워드만 제안한다. 리뷰가
     * 부족하면 리뷰 기반 호출은 생략하고, 대신 AI 일반 지식 기반 키워드를 제안한다 — 실제 리뷰
     * 데이터에 근거한 게 아니라 참고용이다. 리뷰가 충분할 때는 일반지식을 만들지 않아 불필요한
     * AI 호출 비용이 들지 않는다.
     *
     * <p>promptText는 AI 호출 없이 고정 문구로 만든다 — 관리자가 이 결과에 만족하지 못할 때
     * ChatGPT/Gemini 앱 같은 다른 AI 챗봇에 그대로 붙여넣어 물어볼 수 있도록 제공하는 것으로,
     * 매번 다르게 생성되면 품질이 들쭉날쭉해지므로 검증된 문구 하나를 세그먼트만 바꿔 재사용한다.
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

        if (excerpts.size() >= MIN_REVIEWS_FOR_SUGGESTION) {
            List<String> reviewKeywords = keywordSuggester.suggest(segment, excerpts, existingKeywords);
            return new SegmentKeywordSuggestion(reviewKeywords, excerpts.size(), List.of(), null);
        }

        List<String> generalKeywords = keywordSuggester.suggestFallback(segment, existingKeywords);
        String promptText = "%s의 구매 결정 요인 및 소비 트렌드를 한눈에 보기 쉬운 키워드로 정리해 줘."
                .formatted(segment.getLabel());
        return new SegmentKeywordSuggestion(List.of(), excerpts.size(), generalKeywords, promptText);
    }
}
