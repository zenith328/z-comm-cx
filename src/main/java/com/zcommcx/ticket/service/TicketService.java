package com.zcommcx.ticket.service;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.order.domain.Order;
import com.zcommcx.order.service.OrderService;
import com.zcommcx.ticket.domain.Ticket;
import com.zcommcx.ticket.domain.TicketCategory;
import com.zcommcx.ticket.domain.TicketChannel;
import com.zcommcx.ticket.domain.TicketRepository;
import com.zcommcx.ticket.web.dto.TicketCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OrderService orderService;

    @Transactional
    public Ticket createTicket(TicketCreateRequest request) {
        Order order = request.orderId() == null ? null : orderService.getOrder(request.orderId());
        Ticket ticket = buildTicket(
                order, request.category(), request.channel(), request.summary(),
                request.customerName(), request.customerPhone(), null);
        return ticketRepository.save(ticket);
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("티켓을 찾을 수 없습니다. (id=%d)".formatted(id)));
    }

    public List<Ticket> listTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Ticket escalate(Long id) {
        Ticket ticket = getTicket(id);
        ticket.escalate();
        return ticket;
    }

    @Transactional
    public Ticket resolve(Long id, String resolution) {
        Ticket ticket = getTicket(id);
        ticket.resolve(resolution);
        return ticket;
    }

    @Transactional
    public Ticket startProgress(Long id) {
        Ticket ticket = getTicket(id);
        ticket.startProgress();
        return ticket;
    }

    @Transactional
    public Ticket escalateToHuman(Long orderId, TicketCategory category, String summary,
                                   String customerName, String customerPhone, String chatTranscript) {
        Order order = orderId == null ? null : orderService.getOrder(orderId);
        Ticket ticket = buildTicket(order, category, TicketChannel.AI, summary, customerName, customerPhone, chatTranscript);
        ticket.escalate();
        return ticketRepository.save(ticket);
    }

    /**
     * AI가 가드레일을 통과해 직접 처리를 완료한 요청을 CS 이력으로 남긴다 (이미 처리 완료된 상태이므로 CLOSED로 저장).
     */
    @Transactional
    public Ticket recordAiResolvedAction(Long orderId, TicketCategory category, String summary, String resolution,
                                          String chatTranscript) {
        Order order = orderId == null ? null : orderService.getOrder(orderId);
        Ticket ticket = buildTicket(order, category, TicketChannel.AI, summary, null, null, chatTranscript);
        ticket.resolve(resolution);
        return ticketRepository.save(ticket);
    }

    /**
     * 주문이 연결되어 있으면 주문의 실제 고객 정보를 우선 사용하고, 그렇지 않으면 전달받은 값(채팅 세션의 로그인 정보)을 사용한다.
     */
    private Ticket buildTicket(Order order, TicketCategory category, TicketChannel channel, String summary,
                                String customerName, String customerPhone, String chatTranscript) {
        String resolvedName = order != null ? order.getCustomerName() : customerName;
        String resolvedPhone = order != null ? order.getCustomerPhone() : customerPhone;
        return new Ticket(generateTicketNo(), order, category, channel, summary, resolvedName, resolvedPhone, chatTranscript);
    }

    private String generateTicketNo() {
        return "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
