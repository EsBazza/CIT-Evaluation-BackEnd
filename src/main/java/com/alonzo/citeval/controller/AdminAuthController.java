package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.dto.LoginRequestDTO;
import com.alonzo.citeval.model.dto.LoginResponseDTO;
import com.alonzo.citeval.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/admin-login")
    public LoginResponseDTO adminLogin(@Valid @RequestBody LoginRequestDTO req) {
        String token = authService.authenticateAdmin(req.username(), req.password());

        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return new LoginResponseDTO(token);
    }
}
