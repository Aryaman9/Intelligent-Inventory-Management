package com.inventory.service;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RefreshTokenRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.TokenRefreshResponse;
import com.inventory.entity.Role;
import com.inventory.entity.SubscriptionPlan;
import com.inventory.entity.User;
import com.inventory.exception.ConflictException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.repository.jpa.UserRepository;
import com.inventory.security.JwtTokenProvider;
import com.inventory.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AuthService authService;

    private User userWithId(String email, String passwordHash, int failedAttempts, LocalDateTime lockedUntil) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .fullName("Test User")
                .role(Role.RETAILER)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .failedLoginAttempts(failedAttempts)
                .accountLockedUntil(lockedUntil)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void register_success_returnsTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.com").password("password123").fullName("New User").build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("new@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .email("dup@test.com").password("password123").fullName("Dup").build();
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_resetsFailedAttempts() {
        User user = userWithId("ok@test.com", "hashed", 3, null);
        LoginRequest request = new LoginRequest();
        request.setEmail("ok@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getAccountLockedUntil()).isNull();
    }

    @Test
    void login_wrongPassword_incrementsAttemptsAndThrows() {
        User user = userWithId("bad@test.com", "hashed", 0, null);
        LoginRequest request = new LoginRequest();
        request.setEmail("bad@test.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("bad@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getAccountLockedUntil()).isNull();
    }

    @Test
    void login_fifthFailure_locksAccount() {
        User user = userWithId("lock@test.com", "hashed", 4, null);
        LoginRequest request = new LoginRequest();
        request.setEmail("lock@test.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("lock@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLockedUntil()).isNotNull();
    }

    @Test
    void login_lockedAccount_throwsWithLockedMessage() {
        User user = userWithId("locked@test.com", "hashed", 5, LocalDateTime.now().plusMinutes(10));
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("locked@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("locked");
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshToken_success_rotatesAndBlacklistsOldJti() {
        User user = userWithId("refresh@test.com", "hashed", 0, null);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh");

        when(jwtTokenProvider.validateRefreshToken("old-refresh")).thenReturn(true);
        when(jwtTokenProvider.extractJtiFromRefresh("old-refresh")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(jwtTokenProvider.extractUserIdFromRefresh("old-refresh"))
                .thenReturn(user.getId().toString());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.getRemainingRefreshTtlMs("old-refresh")).thenReturn(5000L);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh");

        TokenRefreshResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(tokenBlacklistService).blacklist(eq("jti-1"), anyLong());
    }


    @Test
    void refreshToken_blacklistedToken_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-refresh");

        when(jwtTokenProvider.validateRefreshToken("revoked-refresh")).thenReturn(true);
        when(jwtTokenProvider.extractJtiFromRefresh("revoked-refresh")).thenReturn("jti-2");
        when(tokenBlacklistService.isBlacklisted("jti-2")).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class);
        verify(userRepository, never()).findById(any());
    }
}
