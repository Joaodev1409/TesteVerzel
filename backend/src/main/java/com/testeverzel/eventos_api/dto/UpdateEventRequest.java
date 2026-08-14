package com.testeverzel.eventos_api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * O mapa de assentos não entra aqui: alterar fileiras depois que existem reservas mudaria
 * o significado de ingressos já vendidos.
 */
public record UpdateEventRequest(
        @NotBlank String titulo,
        String sinopse,
        @NotNull @Future OffsetDateTime data,
        @NotBlank String local,
        @NotNull @DecimalMin("0.00") BigDecimal precoBase,
        Long tmdbId) {
}
