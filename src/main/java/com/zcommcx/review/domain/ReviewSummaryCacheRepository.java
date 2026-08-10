package com.zcommcx.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ReviewSummaryCacheRepository extends JpaRepository<ReviewSummaryCache, Long> {

    Optional<ReviewSummaryCache> findByProductCodeAndQuery(String productCode, String query);

    @Transactional
    void deleteByProductCode(String productCode);
}
