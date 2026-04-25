package com.ragchat.service;

import com.ragchat.dto.request.LoginRequest;
import com.ragchat.dto.request.RegisterRequest;
import com.ragchat.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
