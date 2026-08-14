package com.testeverzel.eventos_api.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.domain.Reservation;
import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.Ticket;
import com.testeverzel.eventos_api.dto.ConfirmReservationRequest;
import com.testeverzel.eventos_api.dto.HoldSeatRequest;
import com.testeverzel.eventos_api.dto.ReservationResponse;
import com.testeverzel.eventos_api.dto.TicketResponse;
import com.testeverzel.eventos_api.security.QrCodeSigner;
import com.testeverzel.eventos_api.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReservationController {

    private final ReservationService reservationService;
    private final QrCodeSigner qrCodeSigner;

    public ReservationController(ReservationService reservationService, QrCodeSigner qrCodeSigner) {
        this.reservationService = reservationService;
        this.qrCodeSigner = qrCodeSigner;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse hold(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HoldSeatRequest request) {
        Reservation reservation = reservationService.holdSeat(
                request.eventId(), request.seatId(), UUID.fromString(jwt.getSubject()));
        return ReservationResponse.from(reservation);
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Object> confirm(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ConfirmReservationRequest request) {
        Optional<Ticket> ticket = reservationService.confirmReservation(
                reservationId, UUID.fromString(jwt.getSubject()), request.paymentSuccessful());

        if (ticket.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Payment declined; reservation is still pending and can be retried until it expires");
            return ResponseEntity.of(problem).build();
        }
        return ResponseEntity.ok(toResponse(ticket.get()));
    }

    private TicketResponse toResponse(Ticket ticket) {
        Seat seat = ticket.getReservation().getSeat();
        UUID eventId = seat.getEvent().getId();
        return new TicketResponse(
                ticket.getId(),
                ticket.getReservation().getId(),
                eventId,
                seat.getId(),
                qrCodeSigner.encodeQrCode(ticket.getId(), eventId, seat.getId()));
    }
}
