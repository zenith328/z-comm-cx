package com.zcommcx.dbmonitor.service;

import com.zcommcx.review.domain.ReviewSummaryCacheRepository;
import com.zcommcx.ticket.domain.TicketRepository;
import com.zcommcx.ticket.domain.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Supabase(Postgres) 무료 플랜 용량(500MB) 관리용. Postgres 자체 카탈로그를 조회하므로
 * Supabase API 없이 우리 DB 커넥션만으로 조회할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class DbUsageService {

    private final JdbcTemplate jdbcTemplate;
    private final ReviewSummaryCacheRepository reviewSummaryCacheRepository;
    private final TicketRepository ticketRepository;

    public long totalBytes() {
        Long bytes = jdbcTemplate.queryForObject("select pg_database_size(current_database())", Long.class);
        return bytes != null ? bytes : 0L;
    }

    /** 용량을 많이 차지하는 순서로 테이블 목록을 반환한다 (row 수는 정확한 COUNT가 아닌 통계 추정치). */
    public List<TableUsage> topTables(int limit) {
        return jdbcTemplate.query(
                "select relname as table_name, pg_total_relation_size(relid) as total_bytes, "
                        + "n_live_tup as row_estimate "
                        + "from pg_stat_user_tables order by total_bytes desc limit ?",
                (rs, rowNum) -> new TableUsage(
                        rs.getString("table_name"), rs.getLong("total_bytes"), rs.getLong("row_estimate")),
                limit);
    }

    /** AI 리뷰 요약 캐시는 언제든 다시 생성 가능한 순수 캐시라 통째로 지워도 안전하다. */
    @Transactional
    public long clearReviewSummaryCache() {
        long count = reviewSummaryCacheRepository.count();
        reviewSummaryCacheRepository.deleteAll();
        return count;
    }

    /** 티켓 자체는 남기고, 처리완료 후 olderThanDays일이 지난 티켓의 대화록만 지운다. */
    @Transactional
    public int purgeOldTicketTranscripts(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return ticketRepository.clearChatTranscripts(TicketStatus.CLOSED, cutoff);
    }
}
