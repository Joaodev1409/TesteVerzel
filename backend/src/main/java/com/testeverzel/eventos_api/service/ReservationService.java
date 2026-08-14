package com.testeverzel.eventos_api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testeverzel.eventos_api.domain.Reservation;
import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.Ticket;
import com.testeverzel.eventos_api.domain.enums.ReservationStatus;
import com.testeverzel.eventos_api.domain.enums.SeatStatus;
import com.testeverzel.eventos_api.exception.InvalidReservationStateException;
import com.testeverzel.eventos_api.exception.PaymentDeclinedException;
import com.testeverzel.eventos_api.exception.ReservationExpiredException;
import com.testeverzel.eventos_api.exception.ReservationNotFoundException;
import com.testeverzel.eventos_api.exception.SeatNotAvailableException;
import com.testeverzel.eventos_api.exception.SeatNotFoundException;
import com.testeverzel.eventos_api.payment.PaymentGatewaySimulator;
import com.testeverzel.eventos_api.payment.PaymentResult;
import com.testeverzel.eventos_api.repository.ReservationRepository;
import com.testeverzel.eventos_api.repository.SeatRepository;
import com.testeverzel.eventos_api.repository.TicketRepository;
import com.testeverzel.eventos_api.repository.UserRepository;
import com.testeverzel.eventos_api.security.QrCodeSigner;

@Service
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final QrCodeSigner qrCodeSigner;
    private final PaymentGatewaySimulator paymentGateway;
    private final Duration holdDuration;

    public ReservationService(
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            QrCodeSigner qrCodeSigner,
            PaymentGatewaySimulator paymentGateway,
            @Value("${app.reservation.hold-duration}") Duration holdDuration) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.qrCodeSigner = qrCodeSigner;
        this.paymentGateway = paymentGateway;
        this.holdDuration = holdDuration;
    }

    @Transactional
    public Reservation holdSeat(UUID eventId, UUID seatId, UUID userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .filter(s -> s.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new SeatNotAvailableException(seatId, seat.getStatus());
        }

        seat.setStatus(SeatStatus.HELD);

        Reservation reservation = Reservation.builder()
                .seat(seat)
                .user(userRepository.getReferenceById(userId))
                .status(ReservationStatus.PENDING)
                .expiresAt(Instant.now().plus(holdDuration))
                .build();

        return reservationRepository.save(reservation);
    }

    // noRollbackFor keeps the lazy expiration below committed; without it the throw would undo the
    // very release it just performed. A declined payment mutates nothing, so it rolls back harmlessly.
    @Transactional(noRollbackFor = { ReservationExpiredException.class, PaymentDeclinedException.class })
    public Ticket confirmReservation(UUID reservationId, UUID userId, String cardNumber) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Ownership check reports "not found" instead of "forbidden" to avoid leaking that the id exists.
        if (!reservation.getUser().getId().equals(userId)) {
            throw new ReservationNotFoundException(reservationId);
        }

        if (reservation.getStatus() == ReservationStatus.PENDING
                && reservation.getExpiresAt().isBefore(Instant.now())) {
            expireAndReleaseSeat(reservation);
            throw new ReservationExpiredException(reservationId);
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(reservationId, reservation.getStatus());
        }

        Seat seat = reservation.getSeat();

        // The outcome comes from the gateway, never from the caller.
        PaymentResult payment = paymentGateway.charge(cardNumber, seat.getEvent().getPrecoBase());
        if (!payment.approved()) {
            throw new PaymentDeclinedException(payment.declineReason());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        seat.setStatus(SeatStatus.SOLD);

        UUID ticketId = UUID.randomUUID();
        String qrCodeHash = qrCodeSigner.sign(ticketId, seat.getEvent().getId(), seat.getId());

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .reservation(reservation)
                .qrCodeHash(qrCodeHash)
                .build();

        return ticketRepository.save(ticket);
    }

    /**
     * Desistência antes do pagamento: devolve o assento ao estoque na hora, sem esperar a expiração.
     * Só reservas pendentes podem ser canceladas — depois de confirmada existe um ingresso emitido,
     * e desfazer isso exigiria estorno, que está fora do escopo.
     */
    @Transactional
    public void cancelReservation(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new ReservationNotFoundException(reservationId);
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(reservationId, reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.getSeat().setStatus(SeatStatus.AVAILABLE);
    }

    @Scheduled(fixedDelayString = "${app.reservation.expiration-sweep-interval-ms}")
    @Transactional
    public void expirePendingReservations() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING, Instant.now());
        expired.forEach(this::expireAndReleaseSeat);
    }

    private void expireAndReleaseSeat(Reservation reservation) {
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.getSeat().setStatus(SeatStatus.AVAILABLE);
    }
}
