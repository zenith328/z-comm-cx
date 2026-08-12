package com.zcommcx.aiusage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gemini API 하루 호출 횟수(RPD)·토큰 사용량 누적 기록. 날짜(태평양시 기준, Google 한도 초기화
 * 기준과 동일) 하나당 행 하나이며, 호출마다 {@link GeminiApiUsageRepository#incrementUsage}로
 * request_count/token_count를 원자적으로 누적한다. RPD는 한도(100)를 실제로 표시하지만,
 * 토큰(TPM)은 한도 제어 없이 참고용으로만 누적한다.
 * 행 생성/증가는 항상 네이티브 upsert 쿼리로만 이뤄지므로, 별도의 생성자는 두지 않는다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "gemini_api_usage")
public class GeminiApiUsage {

    @Id
    private LocalDate usageDate;

    @Column(nullable = false)
    private int requestCount;

    @Column(nullable = false)
    private long tokenCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
