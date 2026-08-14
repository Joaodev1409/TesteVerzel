package com.testeverzel.eventos_api.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MyTicketResponse(
        UUID id,
        UUID eventId,
        String eventTitulo,
        OffsetDateTime eventData,
        String eventLocal,
        String fileira,
        Integer numero,
        String qrCode,
        Instant usedAt) {
}
