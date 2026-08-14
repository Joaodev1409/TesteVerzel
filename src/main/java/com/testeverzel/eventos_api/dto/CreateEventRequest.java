package com.testeverzel.eventos_api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
        @NotBlank String titulo,
        String sinopse,
        @NotNull @Future OffsetDateTime data,
        @NotBlank String local,
        @NotNull @DecimalMin("0.00") BigDecimal precoBase,
        Long tmdbId,
        @NotEmpty @Valid List<SeatRowRequest> fileiras) {
}
