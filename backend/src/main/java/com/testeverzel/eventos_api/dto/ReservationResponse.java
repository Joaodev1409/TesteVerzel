package com.testeverzel.eventos_api.dto;

import java.time.Instant;
import java.util.UUID;

import com.testeverzel.eventos_api.domain.Reservation;
import com.testeverzel.eventos_api.domain.enums.ReservationStatus;

public record ReservationResponse(
        UUID id,
        UUID eventId,
        UUID seatId,
        ReservationStatus status,
        Instant expiresAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeat().getEvent().getId(),
                reservation.getSeat().getId(),
                reservation.getStatus(),
                reservation.getExpiresAt());
    }
}
