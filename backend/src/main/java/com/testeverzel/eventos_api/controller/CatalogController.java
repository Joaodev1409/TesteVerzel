package com.testeverzel.eventos_api.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.testeverzel.eventos_api.client.TmdbClient;
import com.testeverzel.eventos_api.dto.MovieSummary;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/catalog")
@PreAuthorize("hasRole('ORGANIZER')")
public class CatalogController {

    private final TmdbClient tmdbClient;

    public CatalogController(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @GetMapping("/movies")
    public List<MovieSummary> searchMovies(@RequestParam @NotBlank String query) {
        return tmdbClient.searchMovies(query);
    }
}
