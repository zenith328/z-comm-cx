package com.zcommcx.personalization.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentKeywordRepository extends JpaRepository<SegmentKeyword, CustomerSegment> {
}
