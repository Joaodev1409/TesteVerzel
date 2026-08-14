package com.testeverzel.eventos_api.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.testeverzel.eventos_api.dto.MovieSummary;
import com.testeverzel.eventos_api.exception.TmdbNotConfiguredException;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(RestClient.Builder builder,
            @Value("${app.tmdb.base-url}") String baseUrl,
            @Value("${app.tmdb.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<MovieSummary> searchMovies(String query) {
        if (apiKey.isBlank()) {
            throw new TmdbNotConfiguredException();
        }

        TmdbSearchResponse response = restClient.get()
                .uri(uri -> uri.path("/search/movie")
                        .queryParam("query", query)
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(TmdbSearchResponse.class);

        return response == null ? List.of() : response.results();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbSearchResponse(List<MovieSummary> results) {
    }
}
