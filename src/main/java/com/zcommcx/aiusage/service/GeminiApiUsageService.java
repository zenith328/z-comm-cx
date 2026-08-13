package com.zcommcx.aiusage.service;

import com.zcommcx.aiusage.domain.GeminiApiUsage;
import com.zcommcx.aiusage.domain.GeminiApiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Gemini API 호출 횟수/토큰 사용량을 하루 단위로 누적 기록한다. 세 곳의 GeminiClient(CS챗봇/
 * 리뷰분석/상품설명 개인화)가 응답을 받은 직후 이 서비스를 호출한다. 사용량 기록 실패가
 * 실제 AI 기능 자체를 막으면 안 되므로 내부에서 예외를 삼키고 로그만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiApiUsageService {

    // Gemini 무료 등급 한도(RPM/TPM/RPD)는 서버 로컬 시간이 아니라 태평양시(PT) 자정 기준으로
    // 초기화된다. America/Los_Angeles는 DST(PDT/PST) 전환도 자동으로 반영한다.
    private static final ZoneId RESET_ZONE = ZoneId.of("America/Los_Angeles");

    private final GeminiApiUsageRepository repository;

    @Transactional
    public void recordRequest(long tokenCount) {
        try {
            repository.incrementUsage(today(), tokenCount, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Gemini API 사용량 기록에 실패했습니다.", e);
        }
    }

    /** Google의 한도 초기화 기준(태평양시 자정)에 맞춘 "오늘" 날짜. */
    public LocalDate today() {
        return LocalDate.now(RESET_ZONE);
    }

    public int countOn(LocalDate date) {
        return repository.findById(date).map(GeminiApiUsage::getRequestCount).orElse(0);
    }

    public long tokensOn(LocalDate date) {
        return repository.findById(date).map(GeminiApiUsage::getTokenCount).orElse(0L);
    }

    public List<GeminiApiUsage> recentHistory(int days) {
        return repository.findAllByOrderByUsageDateDesc(PageRequest.of(0, days));
    }
}
