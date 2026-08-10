package com.zcommcx.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findByProductCodeAndVisibleTrueOrderByCreatedAtDesc(String productCode);

    long countByProductCodeAndVisibleTrue(String productCode);

    List<Review> findByClassificationAndVisibleTrue(ReviewClassification classification);
}
