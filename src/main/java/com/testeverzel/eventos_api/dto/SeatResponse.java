package com.testeverzel.eventos_api.dto;

import java.util.UUID;

import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.enums.SeatStatus;

public record SeatResponse(UUID id, String fileira, Integer numero, SeatStatus status) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getFileira(), seat.getNumero(), seat.getStatus());
    }
}
