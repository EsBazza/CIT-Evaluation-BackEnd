package com.alonzo.citeval.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

        try {
            byte[] secretBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize JWT signing key", ex);
        }
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
