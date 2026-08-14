package com.testeverzel.eventos_api.exception;

import java.time.Instant;
import java.util.UUID;

public class TicketAlreadyUsedException extends RuntimeException {

    public TicketAlreadyUsedException(UUID ticketId, Instant usedAt) {
        super("Ticket " + ticketId + " was already used at " + usedAt);
    }
}
