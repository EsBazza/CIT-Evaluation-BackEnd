package com.alonzo.citeval.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class AuthService {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:root}")
    private String adminPassword;

    // Standard JWT Secret Key (generated securely for demo)
    private final Key signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expirationTimeMs = 86400000; // 24 hours

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
