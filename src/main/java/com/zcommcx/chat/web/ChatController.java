package com.zcommcx.chat.web;

import com.zcommcx.chat.service.ChatService;
import com.zcommcx.chat.web.dto.ChatRequest;
import com.zcommcx.chat.web.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cs/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.sessionId();
        String reply = chatService.chat(sessionId, request.message(), request.customerName(), request.customerPhone());
        return new ChatResponse(sessionId, reply);
    }
}
