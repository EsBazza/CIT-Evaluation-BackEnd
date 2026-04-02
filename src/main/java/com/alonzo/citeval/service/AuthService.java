package com.alonzo.citeval.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.Key;
import java.util.Date;

@Service
public class AuthService {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:root}")
    private String adminPassword;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    private final long expirationTimeMs = 86400000; // 24 hours

    private Key signingKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("APP_JWT_SECRET is required for production deployment");
        }

        String candidate = jwtSecret.trim();
        byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(candidate);
        } catch (IllegalArgumentException ex) {
            secretBytes = candidate.getBytes(StandardCharsets.UTF_8);
        }

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String authenticateAdmin(String username, String password) {
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            return generateToken(username);
        }
        return null;
    }

    // Task 3: Generate proper JWT instead of hardcoded string
    private String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(signingKey)
                .compact();
    }

    // Used by JwtAuthenticationFilter to verify the session
    public String validateTokenAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null; // Token is invalid or expired
        }
    }
}
