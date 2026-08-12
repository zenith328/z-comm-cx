package com.zcommcx.aiusage.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GeminiApiUsageRepository extends JpaRepository<GeminiApiUsage, LocalDate> {

    List<GeminiApiUsage> findAllByOrderByUsageDateDesc(Pageable pageable);

    /**
     * 오늘 행이 없으면 요청 1건/토큰 tokens로 새로 만들고, 있으면 각각 누적한다. 동시에 여러
     * AI 호출이 몰려도(챗봇/리뷰분석/상세설명 생성 등) DB 레벨에서 원자적으로 처리되도록
     * upsert로 구현했다.
     */
    @Modifying
    @Query(
            value = "insert into gemini_api_usage (usage_date, request_count, token_count, updated_at) "
                    + "values (:date, 1, :tokens, now()) "
                    + "on conflict (usage_date) do update set "
                    + "request_count = gemini_api_usage.request_count + 1, "
                    + "token_count = gemini_api_usage.token_count + :tokens, "
                    + "updated_at = now()",
            nativeQuery = true)
    void incrementUsage(@Param("date") LocalDate date, @Param("tokens") long tokens);
}
