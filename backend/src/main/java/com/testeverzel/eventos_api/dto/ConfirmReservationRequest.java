package com.testeverzel.eventos_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmReservationRequest(
        @NotBlank @Pattern(regexp = "[0-9 ]{13,23}", message = "must contain only digits and spaces")
        String cardNumber) {
}
