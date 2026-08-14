package com.testeverzel.eventos_api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String qrCode,
        // Event the gate is operating; when present, tickets of other events are rejected as WRONG_EVENT
        UUID expectedEventId) {
}
