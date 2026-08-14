package com.testeverzel.eventos_api.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.testeverzel.eventos_api.exception.InvalidQrCodeException;

@Component
public class QrCodeSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKeySpec;

    public QrCodeSigner(@Value("${app.qrcode.secret}") String secret) {
        this.secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String sign(UUID ticketId, UUID eventId, UUID seatId) {
        return hmac(payload(ticketId, eventId, seatId));
    }

    public boolean isValid(UUID ticketId, UUID eventId, UUID seatId, String providedHash) {
        byte[] expected = hmac(payload(ticketId, eventId, seatId)).getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    /**
     * Full QR code content handed to the customer: base64url("ticketId:eventId:seatId:hmac").
     */
    public String encodeQrCode(UUID ticketId, UUID eventId, UUID seatId) {
        String content = payload(ticketId, eventId, seatId) + ":" + sign(ticketId, eventId, seatId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a scanned QR code and verifies its HMAC before anything else touches it.
     */
    public QrCodePayload decodeAndVerify(String qrCode) {
        String content;
        try {
            content = new String(Base64.getUrlDecoder().decode(qrCode), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidQrCodeException();
        }

        String[] parts = content.split(":");
        if (parts.length != 4) {
            throw new InvalidQrCodeException();
        }

        UUID ticketId;
        UUID eventId;
        UUID seatId;
        try {
            ticketId = UUID.fromString(parts[0]);
            eventId = UUID.fromString(parts[1]);
            seatId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new InvalidQrCodeException();
        }

        if (!isValid(ticketId, eventId, seatId, parts[3])) {
            throw new InvalidQrCodeException();
        }
        return new QrCodePayload(ticketId, eventId, seatId);
    }

    private String payload(UUID ticketId, UUID eventId, UUID seatId) {
        return ticketId + ":" + eventId + ":" + seatId;
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKeySpec);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign QR code payload", e);
        }
    }
}
