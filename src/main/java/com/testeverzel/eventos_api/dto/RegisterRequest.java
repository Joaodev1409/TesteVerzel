package com.testeverzel.eventos_api.dto;

import com.testeverzel.eventos_api.domain.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotNull Role role) {
}
