package com.testeverzel.eventos_api.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmReservationRequest(
        @NotNull Boolean paymentSuccessful) {
}
