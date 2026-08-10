package com.zcommcx.ticket.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveTicketRequest(@NotBlank String resolution) {
}
