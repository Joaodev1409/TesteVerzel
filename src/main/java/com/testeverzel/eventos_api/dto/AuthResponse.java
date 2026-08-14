package com.testeverzel.eventos_api.dto;

import com.testeverzel.eventos_api.domain.enums.Role;

public record AuthResponse(String token, String email, Role role) {
}
