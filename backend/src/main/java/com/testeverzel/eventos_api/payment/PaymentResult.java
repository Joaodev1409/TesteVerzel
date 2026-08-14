package com.testeverzel.eventos_api.payment;

public record PaymentResult(boolean approved, String authorizationCode, DeclineReason declineReason) {

    public static PaymentResult approve(String authorizationCode) {
        return new PaymentResult(true, authorizationCode, null);
    }

    public static PaymentResult decline(DeclineReason reason) {
        return new PaymentResult(false, null, reason);
    }
}
