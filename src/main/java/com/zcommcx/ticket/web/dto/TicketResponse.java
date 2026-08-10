package com.zcommcx.ticket.web.dto;

import com.zcommcx.ticket.domain.Ticket;
import com.zcommcx.ticket.domain.TicketCategory;
import com.zcommcx.ticket.domain.TicketChannel;
import com.zcommcx.ticket.domain.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketNo,
        Long orderId,
        String orderNo,
        String customerName,
        String customerPhone,
        TicketCategory category,
        TicketStatus status,
        TicketChannel channel,
        String summary,
        String resolution,
        String chatTranscript,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getOrder() == null ? null : ticket.getOrder().getId(),
                ticket.getOrder() == null ? null : ticket.getOrder().getOrderNo(),
                ticket.getCustomerName(),
                ticket.getCustomerPhone(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getChannel(),
                ticket.getSummary(),
                ticket.getResolution(),
                ticket.getChatTranscript(),
                ticket.getCreatedAt(),
                ticket.getResolvedAt());
    }
}
