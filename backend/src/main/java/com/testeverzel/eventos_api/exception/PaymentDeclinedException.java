package com.testeverzel.eventos_api.exception;

import com.testeverzel.eventos_api.payment.DeclineReason;

public class PaymentDeclinedException extends RuntimeException {

    private final DeclineReason reason;

    public PaymentDeclinedException(DeclineReason reason) {
        super("Payment declined (" + reason + "); the reservation is still pending and can be retried until it expires");
        this.reason = reason;
    }

    public DeclineReason getReason() {
        return reason;
    }
}
