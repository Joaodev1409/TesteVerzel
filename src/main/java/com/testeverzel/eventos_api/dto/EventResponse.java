package com.testeverzel.eventos_api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.testeverzel.eventos_api.domain.Event;

public record EventResponse(
        UUID id,
        String titulo,
        String sinopse,
        OffsetDateTime data,
        String local,
        Integer capacidade,
        BigDecimal precoBase,
        Long tmdbId) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitulo(),
                event.getSinopse(),
                event.getData(),
                event.getLocal(),
                event.getCapacidade(),
                event.getPrecoBase(),
                event.getTmdbId());
    }
}
