package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.dto.request.LoginRequest;
import com.anuraggupta.sqlgenie.dto.request.RegisterRequest;
import com.anuraggupta.sqlgenie.dto.response.AuthResponse;
import com.anuraggupta.sqlgenie.entity.RefreshToken;
import com.anuraggupta.sqlgenie.entity.Role;
import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.exception.EmailAlreadyExistsException;
import com.anuraggupta.sqlgenie.exception.InvalidCredentialsException;
import com.anuraggupta.sqlgenie.exception.InvalidRefreshTokenException;
import com.anuraggupta.sqlgenie.repository.RefreshTokenRepository;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import com.anuraggupta.sqlgenie.security.JwtService;
import com.anuraggupta.sqlgenie.service.AuthService;
import com.anuraggupta.sqlgenie.util.TokenHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            log.warn("Reuse of a revoked refresh token detected for user {} - revoking all sessions",
                    storedToken.getUser().getEmail());
            refreshTokenRepository.revokeAllForUser(storedToken.getUser().getId());
            throw new InvalidRefreshTokenException("Refresh token has already been used and was revoked");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueTokens(storedToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = TokenHashUtil.generateRawToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, "Bearer", accessTokenExpirationMs / 1000);
    }
}
