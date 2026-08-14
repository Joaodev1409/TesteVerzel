package com.testeverzel.eventos_api.exception;

import java.util.UUID;

public class WrongEventException extends RuntimeException {

    public WrongEventException(UUID ticketId) {
        super("Ticket " + ticketId + " belongs to a different event");
    }
}
