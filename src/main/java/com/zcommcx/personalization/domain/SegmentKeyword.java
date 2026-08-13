package com.zcommcx.personalization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 세그먼트(성별×연령)별로 강조할 키워드/포인트. 상품별로 따로 관리하면 상품 수만큼 반복 입력해야 해서
 * 너무 번거로우므로, 세그먼트당 하나씩(전체 상품 공통) 관리한다. 3단계(AI 상세설명 변환)에서
 * 상품마다 이 공통 키워드를 함께 전달해 세그먼트별 설명을 생성한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "segment_keyword")
public class SegmentKeyword {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerSegment segment;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SegmentKeyword(CustomerSegment segment, String keywords) {
        this.segment = segment;
        this.keywords = keywords;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateKeywords(String keywords) {
        this.keywords = keywords;
        this.updatedAt = LocalDateTime.now();
    }
}
