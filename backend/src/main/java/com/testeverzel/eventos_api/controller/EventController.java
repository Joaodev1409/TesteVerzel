package com.testeverzel.eventos_api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.dto.CreateEventRequest;
import com.testeverzel.eventos_api.dto.EventResponse;
import com.testeverzel.eventos_api.dto.SeatResponse;
import com.testeverzel.eventos_api.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateEventRequest request) {
        return eventService.create(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping
    public List<EventResponse> list() {
        return eventService.list();
    }

    @GetMapping("/{eventId}")
    public EventResponse get(@PathVariable UUID eventId) {
        return eventService.get(eventId);
    }

    @GetMapping("/{eventId}/seats")
    public List<SeatResponse> getSeats(@PathVariable UUID eventId) {
        return eventService.getSeats(eventId);
    }
}
