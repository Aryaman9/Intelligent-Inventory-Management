package com.inventory.security;

import com.inventory.config.JwtProperties;
import com.inventory.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("plan", user.getSubscriptionPlan().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTokenExpirationMs()))
                .signWith(getAccessKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getRefreshTokenExpirationMs()))
                .signWith(getRefreshKey())
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validate(token, getAccessKey());
    }

    public boolean validateRefreshToken(String token) {
        return validate(token, getRefreshKey());
    }

    public String extractUserId(String token) {
        return extractClaims(token, getAccessKey()).getSubject();
    }

    public String extractUserIdFromRefresh(String token) {
        return extractClaims(token, getRefreshKey()).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token, getAccessKey()).getId();
    }

    public String extractJtiFromRefresh(String token) {
        return extractClaims(token, getRefreshKey()).getId();
    }

    public long getRemainingAccessTtlMs(String token) {
        return Math.max(0, extractClaims(token, getAccessKey()).getExpiration().getTime() - System.currentTimeMillis());
    }

    public long getRemainingRefreshTtlMs(String token) {
        return Math.max(0, extractClaims(token, getRefreshKey()).getExpiration().getTime() - System.currentTimeMillis());
    }

    private boolean validate(String token, SecretKey key) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessTokenSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshTokenSecret().getBytes(StandardCharsets.UTF_8));
    }
}
