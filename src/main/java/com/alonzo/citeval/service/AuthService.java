package com.alonzo.citeval.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@system}")
    private String adminEmail;

    @Value("${app.admin.password:root}")
    private String adminPassword;

    @Value("${app.admin.token:CIT-EVAL-ADMIN-SECRET-2024}")
    private String adminToken;

    public String authenticateAdmin(String username, String password) {
        String normalizedInputUser = username == null ? "" : username.trim();
        String normalizedInputPassword = password == null ? "" : password.trim();
        String normalizedAdminUsername = adminUsername == null ? "" : adminUsername.trim();
        String normalizedAdminEmail = adminEmail == null ? "" : adminEmail.trim();
        String normalizedAdminPassword = adminPassword == null ? "" : adminPassword.trim();

        boolean usernameMatches = normalizedAdminUsername.equalsIgnoreCase(normalizedInputUser)
                || normalizedAdminEmail.equalsIgnoreCase(normalizedInputUser);
        boolean passwordMatches = normalizedAdminPassword.equals(normalizedInputPassword);

        if (usernameMatches && passwordMatches) {
            return adminToken;
        }
        return null;
    }
}
