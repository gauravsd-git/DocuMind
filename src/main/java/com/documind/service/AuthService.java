package com.documind.service;

import com.documind.dto.AuthResponse;
import com.documind.dto.LoginRequest;
import com.documind.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}