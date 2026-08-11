package com.zcommcx.dbmonitor.web;

import com.zcommcx.dbmonitor.service.DbUsageService;
import com.zcommcx.dbmonitor.web.dto.ClearCacheResponse;
import com.zcommcx.dbmonitor.web.dto.DbUsageResponse;
import com.zcommcx.dbmonitor.web.dto.PurgeTicketTranscriptsRequest;
import com.zcommcx.dbmonitor.web.dto.PurgeTicketTranscriptsResponse;
import com.zcommcx.dbmonitor.web.dto.TableUsageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/db-usage")
@RequiredArgsConstructor
public class DbUsageController {

    // Supabase Free 플랜의 DB 용량 한도. 다른 플랜/DB로 옮기면 이 값만 바꾸면 된다.
    private static final long LIMIT_BYTES = 500L * 1024 * 1024;

    private final DbUsageService dbUsageService;

    @GetMapping
    public DbUsageResponse getUsage() {
        long total = dbUsageService.totalBytes();
        var tables = dbUsageService.topTables(10).stream().map(TableUsageResponse::from).toList();
        return new DbUsageResponse(total, LIMIT_BYTES, tables);
    }

    @PostMapping("/clear-summary-cache")
    public ClearCacheResponse clearSummaryCache() {
        return new ClearCacheResponse(dbUsageService.clearReviewSummaryCache());
    }

    @PostMapping("/purge-ticket-transcripts")
    public PurgeTicketTranscriptsResponse purgeTicketTranscripts(@Valid @RequestBody PurgeTicketTranscriptsRequest request) {
        int cleared = dbUsageService.purgeOldTicketTranscripts(request.olderThanDays());
        return new PurgeTicketTranscriptsResponse(cleared);
    }
}
