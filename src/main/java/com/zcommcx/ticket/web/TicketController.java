package com.zcommcx.ticket.web;

import com.zcommcx.ticket.service.TicketService;
import com.zcommcx.ticket.web.dto.ResolveTicketRequest;
import com.zcommcx.ticket.web.dto.TicketCreateRequest;
import com.zcommcx.ticket.web.dto.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody TicketCreateRequest request) {
        return TicketResponse.from(ticketService.createTicket(request));
    }

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.listTickets().stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(ticketService.getTicket(id));
    }

    @PostMapping("/{id}/escalate")
    public TicketResponse escalate(@PathVariable Long id) {
        return TicketResponse.from(ticketService.escalate(id));
    }

    @PostMapping("/{id}/start-progress")
    public TicketResponse startProgress(@PathVariable Long id) {
        return TicketResponse.from(ticketService.startProgress(id));
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveTicketRequest request) {
        return TicketResponse.from(ticketService.resolve(id, request.resolution()));
    }
}
