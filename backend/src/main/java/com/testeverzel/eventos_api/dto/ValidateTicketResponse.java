package com.testeverzel.eventos_api.dto;

import java.time.Instant;
import java.util.UUID;

public record ValidateTicketResponse(
        UUID ticketId,
        UUID eventId,
        String eventTitulo,
        String fileira,
        Integer numero,
        Instant usedAt) {
}
