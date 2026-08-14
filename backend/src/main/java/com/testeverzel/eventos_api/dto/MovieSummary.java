package com.testeverzel.eventos_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieSummary(
        Long id,
        @JsonProperty("title") String titulo,
        @JsonProperty("overview") String sinopse,
        @JsonProperty("release_date") String dataLancamento,
        @JsonProperty("poster_path") String posterPath) {
}
