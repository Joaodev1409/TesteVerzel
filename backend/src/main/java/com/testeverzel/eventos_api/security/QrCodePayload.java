package com.testeverzel.eventos_api.security;

import java.util.UUID;

public record QrCodePayload(UUID ticketId, UUID eventId, UUID seatId) {
}
