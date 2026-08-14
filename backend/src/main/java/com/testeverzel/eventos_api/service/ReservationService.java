package com.testeverzel.eventos_api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import com.testeverzel.eventos_api.exception.ReservationExpiredException;
import com.testeverzel.eventos_api.exception.ReservationNotFoundException;
import com.testeverzel.eventos_api.exception.SeatNotAvailableException;
import com.testeverzel.eventos_api.exception.SeatNotFoundException;
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
    private final Duration holdDuration;

    public ReservationService(
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            QrCodeSigner qrCodeSigner,
            @Value("${app.reservation.hold-duration}") Duration holdDuration) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.qrCodeSigner = qrCodeSigner;
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

    // Empty result = payment declined; reservation stays PENDING so the customer can retry before the hold expires.
    @Transactional
    public Optional<Ticket> confirmReservation(UUID reservationId, UUID userId, boolean paymentSuccessful) {
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

        if (!paymentSuccessful) {
            return Optional.empty();
        }

        Seat seat = reservation.getSeat();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        seat.setStatus(SeatStatus.SOLD);

        UUID ticketId = UUID.randomUUID();
        String qrCodeHash = qrCodeSigner.sign(ticketId, seat.getEvent().getId(), seat.getId());

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .reservation(reservation)
                .qrCodeHash(qrCodeHash)
                .build();

        return Optional.of(ticketRepository.save(ticket));
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
