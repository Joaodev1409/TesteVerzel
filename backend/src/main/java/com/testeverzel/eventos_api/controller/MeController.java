package com.testeverzel.eventos_api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.dto.EventResponse;
import com.testeverzel.eventos_api.dto.MyTicketResponse;
import com.testeverzel.eventos_api.service.EventService;
import com.testeverzel.eventos_api.service.TicketService;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final TicketService ticketService;
    private final EventService eventService;

    public MeController(TicketService ticketService, EventService eventService) {
        this.ticketService = ticketService;
        this.eventService = eventService;
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<MyTicketResponse> myTickets(@AuthenticationPrincipal Jwt jwt) {
        return ticketService.myTickets(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public List<EventResponse> myEvents(@AuthenticationPrincipal Jwt jwt) {
        return eventService.myEvents(UUID.fromString(jwt.getSubject()));
    }
}
