package com.zcommcx.aiusage.web;

import com.zcommcx.aiusage.config.GeminiUsageProperties;
import com.zcommcx.aiusage.service.GeminiApiUsageService;
import com.zcommcx.aiusage.web.dto.DailyUsageResponse;
import com.zcommcx.aiusage.web.dto.GeminiApiUsageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/gemini-usage")
@RequiredArgsConstructor
public class GeminiApiUsageController {

    private final GeminiApiUsageService service;
    private final GeminiUsageProperties usageProperties;

    @GetMapping
    public GeminiApiUsageResponse getUsage() {
        LocalDate today = service.today();
        DailyUsageResponse todayUsage = DailyUsageResponse.of(today, service.countOn(today), service.tokensOn(today));
        List<DailyUsageResponse> recent = service.recentHistory(14).stream()
                .map(DailyUsageResponse::from)
                .toList();
        return new GeminiApiUsageResponse(
                usageProperties.tier(),
                usageProperties.rpmLimit(),
                usageProperties.tpmLimit(),
                usageProperties.rpdLimit(),
                todayUsage,
                recent);
    }
}
