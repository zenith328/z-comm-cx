package com.zcommcx.shortlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class BestReviewShortlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String weekLabel;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private Long reviewId;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public BestReviewShortlistEntry(String weekLabel, String productCode, Long reviewId, int rank) {
        this.weekLabel = weekLabel;
        this.productCode = productCode;
        this.reviewId = reviewId;
        this.rank = rank;
        this.createdAt = LocalDateTime.now();
    }
}
