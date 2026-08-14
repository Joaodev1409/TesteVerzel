package com.testeverzel.eventos_api.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.Ticket;
import com.testeverzel.eventos_api.dto.ValidateTicketResponse;
import com.testeverzel.eventos_api.exception.InvalidQrCodeException;
import com.testeverzel.eventos_api.exception.TicketAlreadyUsedException;
import com.testeverzel.eventos_api.repository.TicketRepository;
import com.testeverzel.eventos_api.security.QrCodePayload;
import com.testeverzel.eventos_api.security.QrCodeSigner;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QrCodeSigner qrCodeSigner;

    public TicketService(TicketRepository ticketRepository, QrCodeSigner qrCodeSigner) {
        this.ticketRepository = ticketRepository;
        this.qrCodeSigner = qrCodeSigner;
    }

    @Transactional
    public ValidateTicketResponse validate(String qrCode) {
        QrCodePayload payload = qrCodeSigner.decodeAndVerify(qrCode);

        Ticket ticket = ticketRepository.findByIdForUpdate(payload.ticketId())
                .orElseThrow(InvalidQrCodeException::new);

        // Defense in depth: the stored hash must also match a recomputation over the scanned ids,
        // binding this ticket row to exactly this event/seat.
        if (!qrCodeSigner.isValid(payload.ticketId(), payload.eventId(), payload.seatId(),
                ticket.getQrCodeHash())) {
            throw new InvalidQrCodeException();
        }

        if (ticket.getUsedAt() != null) {
            throw new TicketAlreadyUsedException(ticket.getId(), ticket.getUsedAt());
        }

        ticket.setUsedAt(Instant.now());

        Seat seat = ticket.getReservation().getSeat();
        return new ValidateTicketResponse(
                ticket.getId(),
                seat.getEvent().getId(),
                seat.getEvent().getTitulo(),
                seat.getFileira(),
                seat.getNumero(),
                ticket.getUsedAt());
    }
}
