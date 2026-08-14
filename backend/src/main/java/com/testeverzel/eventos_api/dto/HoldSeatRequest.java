package com.testeverzel.eventos_api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record HoldSeatRequest(
        @NotNull UUID eventId,
        @NotNull UUID seatId) {
}
