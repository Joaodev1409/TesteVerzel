package com.testeverzel.eventos_api.exception;

import java.util.UUID;

import com.testeverzel.eventos_api.domain.enums.ReservationStatus;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(UUID reservationId, ReservationStatus currentStatus) {
        super("Reservation " + reservationId + " is not pending (current status: " + currentStatus + ")");
    }
}
