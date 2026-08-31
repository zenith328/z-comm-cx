package com.zcommcx.personalization.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentKeywordHistoryRepository extends JpaRepository<SegmentKeywordHistory, Long> {

    Page<SegmentKeywordHistory> findBySegmentOrderByChangedAtDesc(CustomerSegment segment, Pageable pageable);
}
