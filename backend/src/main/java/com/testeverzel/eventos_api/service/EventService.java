package com.testeverzel.eventos_api.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testeverzel.eventos_api.domain.Event;
import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.enums.SeatStatus;
import com.testeverzel.eventos_api.dto.CreateEventRequest;
import com.testeverzel.eventos_api.dto.EventResponse;
import com.testeverzel.eventos_api.dto.SeatResponse;
import com.testeverzel.eventos_api.exception.EventNotFoundException;
import com.testeverzel.eventos_api.repository.EventRepository;
import com.testeverzel.eventos_api.repository.SeatRepository;
import com.testeverzel.eventos_api.repository.UserRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, SeatRepository seatRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse create(UUID organizerId, CreateEventRequest request) {
        int capacidade = request.fileiras().stream().mapToInt(row -> row.quantidade()).sum();

        Event event = eventRepository.save(Event.builder()
                .titulo(request.titulo())
                .sinopse(request.sinopse())
                .data(request.data())
                .local(request.local())
                .capacidade(capacidade)
                .precoBase(request.precoBase())
                .organizer(userRepository.getReferenceById(organizerId))
                .tmdbId(request.tmdbId())
                .build());

        List<Seat> seats = request.fileiras().stream()
                .flatMap(row -> IntStream.rangeClosed(1, row.quantidade())
                        .mapToObj(numero -> Seat.builder()
                                .event(event)
                                .fileira(row.fileira())
                                .numero(numero)
                                .status(SeatStatus.AVAILABLE)
                                .build()))
                .toList();
        seatRepository.saveAll(seats);

        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> myEvents(UUID organizerId) {
        return eventRepository.findByOrganizerIdOrderByDataAsc(organizerId).stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(EventResponse::from)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        return seatRepository.findByEventId(eventId).stream().map(SeatResponse::from).toList();
    }
}
