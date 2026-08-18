package com.zcommcx.review.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> filter(
            String productCode, Boolean visible, ReviewClassification classification, ReviewStatus status,
            ReviewOrigin origin) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productCode != null && !productCode.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productCode")), "%" + productCode.trim().toLowerCase() + "%"));
            }
            if (visible != null) {
                predicates.add(cb.equal(root.get("visible"), visible));
            }
            if (classification != null) {
                predicates.add(cb.equal(root.get("classification"), classification));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (origin != null) {
                predicates.add(cb.equal(root.get("origin"), origin));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 구매자 화면(고객 리뷰 목록)용. visible=true 리뷰만 대상으로 하고,
     * 정렬은 Pageable의 Sort가 아니라 여기서 query.orderBy로 직접 지정한다
     * (감성 우선순위 정렬은 단순 컬럼 정렬로 표현할 수 없어 CASE 식이 필요하기 때문 —
     * 호출부는 반드시 정렬이 없는 Pageable을 넘겨야 이 orderBy가 덮어써지지 않는다).
     */
    public static Specification<Review> visibleFor(
            String productCode, Boolean hasPhoto, ReviewClassification classification, ReviewSentiment sentiment,
            ReviewSortOption sort) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productCode"), productCode));
            predicates.add(cb.isTrue(root.get("visible")));
            if (hasPhoto != null) {
                predicates.add(cb.equal(root.get("hasPhoto"), hasPhoto));
            }
            if (classification != null) {
                predicates.add(cb.equal(root.get("classification"), classification));
            }
            if (sentiment != null) {
                predicates.add(cb.equal(root.get("sentiment"), sentiment));
            }
            query.orderBy(buildOrder(sort, root, cb));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<Order> buildOrder(ReviewSortOption sort, Root<Review> root, CriteriaBuilder cb) {
        Order latestFirst = cb.desc(root.get("createdAt"));
        return switch (sort) {
            case RATING_HIGH -> List.of(cb.desc(root.get("rating")), latestFirst);
            case RATING_LOW -> List.of(cb.asc(root.get("rating")), latestFirst);
            case POSITIVE_FIRST -> List.of(cb.asc(sentimentRank(root, cb)), latestFirst);
            case NEGATIVE_FIRST -> List.of(cb.desc(sentimentRank(root, cb)), latestFirst);
            case LATEST -> List.of(latestFirst);
        };
    }

    private static Expression<Integer> sentimentRank(Root<Review> root, CriteriaBuilder cb) {
        return cb.<Integer>selectCase()
                .when(cb.equal(root.get("sentiment"), ReviewSentiment.POSITIVE), 0)
                .when(cb.equal(root.get("sentiment"), ReviewSentiment.NEUTRAL), 1)
                .when(cb.equal(root.get("sentiment"), ReviewSentiment.NEGATIVE), 2)
                .otherwise(1);
    }
}
