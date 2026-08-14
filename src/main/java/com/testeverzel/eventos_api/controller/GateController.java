package com.testeverzel.eventos_api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.dto.ValidateTicketRequest;
import com.testeverzel.eventos_api.dto.ValidateTicketResponse;
import com.testeverzel.eventos_api.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/gate")
@PreAuthorize("hasRole('GATE')")
public class GateController {

    private final TicketService ticketService;

    public GateController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/validate")
    public ValidateTicketResponse validate(@Valid @RequestBody ValidateTicketRequest request) {
        return ticketService.validate(request.qrCode());
    }
}
