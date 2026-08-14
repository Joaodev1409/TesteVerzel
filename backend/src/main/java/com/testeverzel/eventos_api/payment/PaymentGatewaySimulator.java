package com.testeverzel.eventos_api.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real payment provider. The outcome is decided here, server-side, from the card
 * data the customer submits — a client can never declare its own payment successful.
 * Test cards follow the usual sandbox conventions so both outcomes stay demonstrable.
 */
@Component
public class PaymentGatewaySimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySimulator.class);

    private static final String DECLINED_CARD_SUFFIX = "0002";
    private static final String INSUFFICIENT_FUNDS_CARD_SUFFIX = "9995";

    public PaymentResult charge(String cardNumber, BigDecimal amount) {
        String digits = cardNumber.replaceAll("\\D", "");
        PaymentResult result = evaluate(digits);

        log.info("Simulated charge of {} on card ending {}: {}",
                amount,
                digits.length() >= 4 ? digits.substring(digits.length() - 4) : "????",
                result.approved() ? "APPROVED " + result.authorizationCode() : result.declineReason());

        return result;
    }

    private PaymentResult evaluate(String digits) {
        if (digits.length() < 13 || digits.length() > 19 || !isLuhnValid(digits)) {
            return PaymentResult.decline(DeclineReason.INVALID_CARD);
        }
        if (digits.endsWith(DECLINED_CARD_SUFFIX)) {
            return PaymentResult.decline(DeclineReason.CARD_DECLINED);
        }
        if (digits.endsWith(INSUFFICIENT_FUNDS_CARD_SUFFIX)) {
            return PaymentResult.decline(DeclineReason.INSUFFICIENT_FUNDS);
        }
        return PaymentResult.approve(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private static boolean isLuhnValid(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
