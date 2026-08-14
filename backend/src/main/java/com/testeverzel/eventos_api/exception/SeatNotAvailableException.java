package com.testeverzel.eventos_api.exception;

import java.util.UUID;

import com.testeverzel.eventos_api.domain.enums.SeatStatus;

public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(UUID seatId, SeatStatus currentStatus) {
        super("Seat " + seatId + " is not available (current status: " + currentStatus + ")");
    }
}
