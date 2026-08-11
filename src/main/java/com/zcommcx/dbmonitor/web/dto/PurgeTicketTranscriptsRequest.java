package com.zcommcx.dbmonitor.web.dto;

import jakarta.validation.constraints.Min;

public record PurgeTicketTranscriptsRequest(@Min(1) int olderThanDays) {
}
