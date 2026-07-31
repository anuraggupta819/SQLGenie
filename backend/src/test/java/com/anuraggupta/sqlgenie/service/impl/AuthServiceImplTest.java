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
import com.anuraggupta.sqlgenie.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900_000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, passwordEncoder, jwtService,
                authenticationManager, ACCESS_TOKEN_EXPIRATION_MS, REFRESH_TOKEN_EXPIRATION_MS);
    }

    private User sampleUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .passwordHash("hashed")
                .fullName("Jane Doe")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void register_savesUserAndReturnsTokens_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Passw0rd!", "Jane Doe");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS / 1000);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Passw0rd!", "Jane Doe");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokens_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("jane@example.com", "Passw0rd!");
        User user = sampleUser();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsInvalidCredentials_whenAuthenticationFails() {
        LoginRequest request = new LoginRequest("jane@example.com", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_rotatesToken_whenValid() {
        User user = sampleUser();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex("raw-token"))
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("raw-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void refresh_throwsAndRevokesAllSessions_whenTokenAlreadyRevoked() {
        User user = sampleUser();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex("raw-token"))
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(user.getId());
    }

    @Test
    void refresh_throws_whenTokenExpired() {
        User user = sampleUser();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex("raw-token"))
                .expiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_throws_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_revokesToken_whenFound() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser())
                .tokenHash(TokenHashUtil.sha256Hex("raw-token"))
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("raw-token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_isNoOp_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }
}
