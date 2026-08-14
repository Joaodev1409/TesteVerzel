package com.testeverzel.eventos_api.exception;

import java.util.UUID;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(UUID seatId) {
        super("Seat not found: " + seatId);
    }
}
