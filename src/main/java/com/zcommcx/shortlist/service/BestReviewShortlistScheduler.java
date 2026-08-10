package com.zcommcx.shortlist.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BestReviewShortlistScheduler {

    private final BestReviewShortlistService shortlistService;

    @Scheduled(cron = "0 0 3 ? * MON")
    public void generateWeeklyShortlist() {
        shortlistService.generateForCurrentWeek();
    }
}
