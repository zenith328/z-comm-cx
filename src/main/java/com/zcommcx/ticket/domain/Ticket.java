package com.zcommcx.ticket.domain;

import com.zcommcx.order.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cs_ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true)
    private String ticketNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketChannel channel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "chat_transcript", columnDefinition = "TEXT")
    private String chatTranscript;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Ticket(String ticketNo, Order order, TicketCategory category, TicketChannel channel, String summary,
                  String customerName, String customerPhone, String chatTranscript) {
        this.ticketNo = ticketNo;
        this.order = order;
        this.category = category;
        this.status = TicketStatus.OPEN;
        this.channel = channel;
        this.summary = summary;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.chatTranscript = chatTranscript;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void escalate() {
        this.channel = TicketChannel.HUMAN;
        this.status = TicketStatus.ESCALATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void startProgress() {
        this.status = TicketStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(String resolution) {
        this.resolution = resolution;
        this.status = TicketStatus.CLOSED;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
