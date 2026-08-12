package com.zcommcx.aiusage.web.dto;

import java.util.List;

public record GeminiApiUsageResponse(
        String tier,
        int rpmLimit,
        int tpmLimit,
        int limitPerDay,
        DailyUsageResponse today,
        List<DailyUsageResponse> recent) {
}
