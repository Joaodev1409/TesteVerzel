package com.testeverzel.eventos_api.exception;

public class InvalidQrCodeException extends RuntimeException {

    public InvalidQrCodeException() {
        super("QR code is invalid or has been tampered with");
    }
}
