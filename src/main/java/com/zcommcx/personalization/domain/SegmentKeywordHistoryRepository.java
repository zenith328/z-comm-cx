package com.zcommcx.personalization.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentKeywordHistoryRepository extends JpaRepository<SegmentKeywordHistory, Long> {

    List<SegmentKeywordHistory> findBySegmentOrderByChangedAtDesc(CustomerSegment segment);
}
