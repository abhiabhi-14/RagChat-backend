package com.ragchat.service.impl;

import com.ragchat.dto.request.LoginRequest;
import com.ragchat.dto.request.RegisterRequest;
import com.ragchat.dto.response.AuthResponse;
import com.ragchat.exception.EmailAlreadyExistsException;
import com.ragchat.exception.InvalidCredentialsException;
import com.ragchat.model.User;
import com.ragchat.repository.UserRepository;
import com.ragchat.security.JwtUtil;
import com.ragchat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration and login.
 *
 * Security measures:
 * - Passwords hashed with BCrypt (strength 12) before storage
 * - Generic error message on login failure (prevents user enumeration)
 * - JWT issued on both register and login
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Build user with BCrypt-hashed password (Builder pattern)
        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: [{}]", saved.getEmail());

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail());
        return buildAuthResponse(token, saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        // BCrypt comparison — throws if mismatch
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email [{}]", request.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("User logged in: [{}]", user.getEmail());
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return buildAuthResponse(token, user);
    }

    // ── Builder pattern for response assembly ─────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .build();
    }
}
