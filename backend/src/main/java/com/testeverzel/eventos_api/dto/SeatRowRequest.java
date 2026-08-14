package com.testeverzel.eventos_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeatRowRequest(
        @NotBlank @Size(max = 10) String fileira,
        @NotNull @Min(1) @Max(500) Integer quantidade) {
}
