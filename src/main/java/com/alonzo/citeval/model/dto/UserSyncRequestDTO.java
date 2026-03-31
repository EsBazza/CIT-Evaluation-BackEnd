package com.alonzo.citeval.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserSyncRequestDTO(
    @NotBlank @Email String email,
    @NotBlank String name,
    @NotBlank String oauthProvider,
    @NotBlank String oauthSubject,
    String role
) {}
