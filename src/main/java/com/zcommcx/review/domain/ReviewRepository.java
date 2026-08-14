package com.zcommcx.review.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findByProductCodeAndVisibleTrueOrderByCreatedAtDesc(String productCode);

    long countByProductCodeAndVisibleTrue(String productCode);

    List<Review> findByClassificationAndVisibleTrue(ReviewClassification classification);

    /**
     * 세그먼트별 AI 키워드 제안(personalization)이 "이 리뷰를 실제 어느 세그먼트 고객이 썼는지"
     * 판별하려면 작성 시점 성별 스냅샷이 있어야 한다(연령은 forGenderAndAge에서 null이면 자동
     * 제외됨). 최신순으로 넉넉히 가져와서 호출자가 세그먼트 매칭 필터링을 한 뒤 필요한 만큼만 쓴다.
     */
    List<Review> findByGenderAtCreationIsNotNullAndVisibleTrueOrderByCreatedAtDesc(Pageable pageable);
}
