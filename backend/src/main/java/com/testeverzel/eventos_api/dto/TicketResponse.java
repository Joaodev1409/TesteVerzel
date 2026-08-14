package com.testeverzel.eventos_api.dto;

import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID reservationId,
        UUID eventId,
        UUID seatId,
        String qrCode) {
}
