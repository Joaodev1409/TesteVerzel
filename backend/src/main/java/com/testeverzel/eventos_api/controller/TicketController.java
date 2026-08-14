package com.testeverzel.eventos_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.dto.MyTicketResponse;
import com.testeverzel.eventos_api.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Público por definição: o link compartilhado precisa abrir sem conta. */
    @GetMapping("/shared/{qrCode}")
    public MyTicketResponse shared(@PathVariable String qrCode) {
        return ticketService.findShared(qrCode);
    }
}
