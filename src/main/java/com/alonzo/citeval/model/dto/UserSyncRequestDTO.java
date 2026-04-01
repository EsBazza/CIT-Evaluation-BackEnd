package com.alonzo.citeval.model.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSyncRequestDTO(
    @NotBlank String idToken // Accepting Google's ID Token instead of raw user info
) {}
