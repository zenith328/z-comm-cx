package com.zcommcx.personalization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@link SegmentKeyword}는 세그먼트당 최신 키워드만 갖고 있어 "언제 어떤 키워드였는지"를 알 수 없다.
 * 저장(upsert)이 일어날 때마다 그 시점의 스냅샷을 여기 쌓아 변경 이력을 남긴다. 기존
 * segment_keyword 테이블/조회 로직은 그대로 두고, 이력은 별도 테이블에 추가로만 적재한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "segment_keyword_history")
public class SegmentKeywordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CustomerSegment segment;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    public SegmentKeywordHistory(CustomerSegment segment, String keywords) {
        this.segment = segment;
        this.keywords = keywords;
        this.changedAt = LocalDateTime.now();
    }
}
