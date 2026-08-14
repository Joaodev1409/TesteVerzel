package com.testeverzel.eventos_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String qrCode) {
}
