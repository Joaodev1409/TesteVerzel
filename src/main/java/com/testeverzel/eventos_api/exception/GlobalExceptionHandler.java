package com.testeverzel.eventos_api.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ SeatNotFoundException.class, ReservationNotFoundException.class,
            EventNotFoundException.class })
    public ProblemDetail handleNotFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({ SeatNotAvailableException.class, InvalidReservationStateException.class,
            EmailAlreadyInUseException.class, TicketAlreadyUsedException.class })
    public ProblemDetail handleConflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public ProblemDetail handleGone(ReservationExpiredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
    }

    @ExceptionHandler(InvalidQrCodeException.class)
    public ProblemDetail handleInvalidQrCode(InvalidQrCodeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(TmdbNotConfiguredException.class)
    public ProblemDetail handleTmdbNotConfigured(TmdbNotConfiguredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleUpstreamFailure(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Upstream service failed: " + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }
}
