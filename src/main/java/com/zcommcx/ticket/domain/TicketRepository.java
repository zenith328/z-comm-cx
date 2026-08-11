package com.zcommcx.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNo(String ticketNo);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    /**
     * DB 용량 정리용. 티켓 자체(요약/처리결과/일시 등 감사 기록)는 남기고, 용량을 많이 차지하는
     * 원문 대화록(chatTranscript)만 지운다 — 처리 완료(CLOSED)되고 일정 기간이 지난 것만 대상.
     */
    @Modifying
    @Transactional
    @Query("update Ticket t set t.chatTranscript = null "
            + "where t.status = :status and t.resolvedAt < :cutoff and t.chatTranscript is not null")
    int clearChatTranscripts(@Param("status") TicketStatus status, @Param("cutoff") LocalDateTime cutoff);
}
