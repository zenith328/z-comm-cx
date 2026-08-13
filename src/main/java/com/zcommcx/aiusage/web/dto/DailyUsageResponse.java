package com.zcommcx.aiusage.web.dto;

import com.zcommcx.aiusage.domain.GeminiApiUsage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * usage_date는 Google 한도 초기화 기준인 태평양시(PT) 기준 날짜다. 화면에서 헷갈리지 않도록
 * 그 하루(PT 자정~다음날 자정)가 한국시간으로는 몇 시부터 몇 시까지인지도 함께 내려준다.
 */
public record DailyUsageResponse(
        LocalDate date,
        LocalDateTime koreaRangeStart,
        LocalDateTime koreaRangeEnd,
        int requestCount,
        long tokenCount) {

    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    public static DailyUsageResponse from(GeminiApiUsage usage) {
        return of(usage.getUsageDate(), usage.getRequestCount(), usage.getTokenCount());
    }

    public static DailyUsageResponse of(LocalDate date, int requestCount, long tokenCount) {
        ZonedDateTime startOfDayPt = date.atStartOfDay(PACIFIC);
        LocalDateTime koreaStart = startOfDayPt.withZoneSameInstant(KOREA).toLocalDateTime();
        LocalDateTime koreaEnd = startOfDayPt.plusDays(1).withZoneSameInstant(KOREA).toLocalDateTime();
        return new DailyUsageResponse(date, koreaStart, koreaEnd, requestCount, tokenCount);
    }
}
