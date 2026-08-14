package com.testeverzel.eventos_api.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testeverzel.eventos_api.domain.User;
import com.testeverzel.eventos_api.dto.AuthResponse;
import com.testeverzel.eventos_api.dto.LoginRequest;
import com.testeverzel.eventos_api.dto.RegisterRequest;
import com.testeverzel.eventos_api.exception.EmailAlreadyInUseException;
import com.testeverzel.eventos_api.repository.UserRepository;
import com.testeverzel.eventos_api.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new EmailAlreadyInUseException(request.email());
        });

        User user = userRepository.save(User.builder()
                .email(request.email())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .role(request.role())
                .build());

        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.senha(), user.getSenhaHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getRole());
    }
}
