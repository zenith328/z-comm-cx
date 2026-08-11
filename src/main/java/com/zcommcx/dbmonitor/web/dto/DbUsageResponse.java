package com.zcommcx.dbmonitor.web.dto;

import java.util.List;

public record DbUsageResponse(long totalBytes, long limitBytes, List<TableUsageResponse> tables) {
}
