package com.zcommcx.shortlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BestReviewShortlistRepository extends JpaRepository<BestReviewShortlistEntry, Long> {

    List<BestReviewShortlistEntry> findByWeekLabelOrderByProductCodeAscRankAsc(String weekLabel);

    List<BestReviewShortlistEntry> findByWeekLabelAndProductCodeOrderByRankAsc(String weekLabel, String productCode);

    void deleteByWeekLabel(String weekLabel);
}
