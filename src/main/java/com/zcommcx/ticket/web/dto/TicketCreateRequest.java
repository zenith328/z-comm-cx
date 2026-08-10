package com.zcommcx.ticket.web.dto;

import com.zcommcx.ticket.domain.TicketCategory;
import com.zcommcx.ticket.domain.TicketChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketCreateRequest(
        Long orderId,
        @NotNull TicketCategory category,
        @NotNull TicketChannel channel,
        @NotBlank String summary,
        String customerName,
        String customerPhone) {
}
