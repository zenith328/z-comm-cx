package com.zcommcx.dbmonitor.web.dto;

import com.zcommcx.dbmonitor.service.TableUsage;

public record TableUsageResponse(String tableName, long bytes, long rowEstimate) {

    public static TableUsageResponse from(TableUsage usage) {
        return new TableUsageResponse(usage.tableName(), usage.bytes(), usage.rowEstimate());
    }
}
