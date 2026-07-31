package com.anuraggupta.sqlgenie.service;

import com.anuraggupta.sqlgenie.dto.request.LoginRequest;
import com.anuraggupta.sqlgenie.dto.request.RegisterRequest;
import com.anuraggupta.sqlgenie.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
