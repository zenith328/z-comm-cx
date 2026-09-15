package com.zcommcx.avatar.service;

import com.zcommcx.avatar.ai.GeminiShowhostGenerator;
import com.zcommcx.avatar.domain.ShowhostScript;
import com.zcommcx.avatar.domain.ShowhostScriptRepository;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import com.zcommcx.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품상세 정보(설명/가격/리뷰요약)를 바탕으로 AI 쇼호스트 방송 대본을 만들고 상품당 1건으로
 * 캐시한다. 실제 24시간 라이브 스트리밍이 아니라, 고객이 접속하는 순간 그 자리에서 생성/재생하는
 * 온디맨드 방식이라 서버는 대본 텍스트만 만들고 음성 합성(TTS)은 브라우저가 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowhostService {

    // 방송 대본의 근거로 쓸 리뷰 요약을, 이미 있는 "AI 리뷰 요약봇" 캐시(ReviewService)와 그대로
    // 공유하기 위해 질문 텍스트를 고정한다 — 상품상세에서 같은 질문으로 먼저 요약해뒀다면
    // Gemini를 다시 호출하지 않고 캐시를 재사용하게 된다.
    private static final String REVIEW_SUMMARY_QUERY = "이 상품의 전반적인 특징과 장단점을 요약해줘";

    private final ShowhostScriptRepository repository;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final GeminiShowhostGenerator generator;

    // FitProfileService.getOrGenerate와 같은 이유로 클래스 레벨 readOnly=true를 여기서 덮어쓴다 —
    // 캐시 미스 시 save()까지 한 트랜잭션 안에서 처리해야 한다(self-invocation이라 별도
    // @Transactional private 메서드로는 프록시를 타지 않는다).
    @Transactional
    public ShowhostScript getOrGenerate(String productCode) {
        return repository.findById(productCode).orElseGet(() -> generateAndCache(productCode));
    }

    @Transactional
    public ShowhostScript regenerate(String productCode) {
        String script = generateScriptText(productCode);
        return repository.findById(productCode)
                .map(existing -> {
                    existing.update(script);
                    return existing;
                })
                .orElseGet(() -> repository.save(new ShowhostScript(productCode, script)));
    }

    public String answerQuestion(String productCode, String question) {
        Product product = productService.getByProductCode(productCode);
        String reviewSummary = reviewSummary(productCode);
        return generator.answerQuestion(product, reviewSummary, question);
    }

    private ShowhostScript generateAndCache(String productCode) {
        String script = generateScriptText(productCode);
        return repository.save(new ShowhostScript(productCode, script));
    }

    private String generateScriptText(String productCode) {
        Product product = productService.getByProductCode(productCode);
        String reviewSummary = reviewSummary(productCode);
        return generator.generateScript(product, reviewSummary);
    }

    private String reviewSummary(String productCode) {
        return reviewService.summarizeVisibleReviews(productCode, REVIEW_SUMMARY_QUERY).summary();
    }
}
