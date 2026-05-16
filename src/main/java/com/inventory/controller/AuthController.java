package com.inventory.controller;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RefreshTokenRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.TokenRefreshResponse;
import com.inventory.dto.response.UserSummaryResponse;
import com.inventory.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) {
        String accessToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String refreshToken = refreshTokenRequest != null ? refreshTokenRequest.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
