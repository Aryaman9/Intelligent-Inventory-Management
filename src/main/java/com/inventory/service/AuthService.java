package com.inventory.service;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RefreshTokenRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.TokenRefreshResponse;
import com.inventory.dto.response.UserSummaryResponse;
import com.inventory.entity.Role;
import com.inventory.entity.SubscriptionPlan;
import com.inventory.entity.User;
import com.inventory.event.UserActionAuditEvent;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ErrorCode;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.repository.jpa.UserRepository;
import com.inventory.security.JwtTokenProvider;
import com.inventory.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists", ErrorCode.AUTH_005);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already in use", ErrorCode.CONF_001);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.RETAILER)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build();
        userRepository.save(user);

        eventPublisher.publishEvent(UserActionAuditEvent.builder()
                .userId(user.getId())
                .action("USER_REGISTERED")
                .resourceType("User")
                .resourceId(user.getId().toString())
                .metadata(Map.of("email", user.getEmail()))
                .build());

        return buildAuthResponse(user);
    }

    // noRollbackFor: failed-attempt increments and lock timestamps must commit even when
    // the method throws UnauthorizedException (Spring rolls back @Transactional on any RuntimeException by default).
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials", ErrorCode.AUTH_001));

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException(
                    "Account locked until " + user.getAccountLockedUntil(), ErrorCode.AUTH_002);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid credentials", ErrorCode.AUTH_001);
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        eventPublisher.publishEvent(UserActionAuditEvent.builder()
                .userId(user.getId())
                .action("USER_LOGIN")
                .resourceType("User")
                .resourceId(user.getId().toString())
                .metadata(Map.of("email", user.getEmail()))
                .build());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token", ErrorCode.AUTH_004);
        }

        String jti = jwtTokenProvider.extractJtiFromRefresh(refreshToken);
        if (tokenBlacklistService.isBlacklisted(jti)) {
            throw new UnauthorizedException("Refresh token has been revoked", ErrorCode.AUTH_004);
        }

        String userId = jwtTokenProvider.extractUserIdFromRefresh(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("User not found", ErrorCode.AUTH_001));

        tokenBlacklistService.blacklist(jti, jwtTokenProvider.getRemainingRefreshTtlMs(refreshToken));

        return TokenRefreshResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(jwtTokenProvider.generateRefreshToken(user))
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        java.util.UUID logoutUserId = null;
        if (accessToken != null) {
            try {
                if (jwtTokenProvider.validateAccessToken(accessToken)) {
                    logoutUserId = java.util.UUID.fromString(jwtTokenProvider.extractUserId(accessToken));
                    tokenBlacklistService.blacklist(
                            jwtTokenProvider.extractJti(accessToken),
                            jwtTokenProvider.getRemainingAccessTtlMs(accessToken)
                    );
                }
            } catch (Exception ignored) {
            }
        }
        if (refreshToken != null) {
            try {
                if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                    tokenBlacklistService.blacklist(
                            jwtTokenProvider.extractJtiFromRefresh(refreshToken),
                            jwtTokenProvider.getRemainingRefreshTtlMs(refreshToken)
                    );
                }
            } catch (Exception ignored) {
            }
        }
        if (logoutUserId != null) {
            eventPublisher.publishEvent(UserActionAuditEvent.builder()
                    .userId(logoutUserId)
                    .action("USER_LOGOUT")
                    .resourceType("User")
                    .resourceId(logoutUserId.toString())
                    .metadata(Map.of())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserSummaryResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(jwtTokenProvider.generateRefreshToken(user))
                .user(toUserSummaryResponse(user))
                .build();
    }

    private UserSummaryResponse toUserSummaryResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .subscriptionPlan(user.getSubscriptionPlan().name())
                .build();
    }
}
