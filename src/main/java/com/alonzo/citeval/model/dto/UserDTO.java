package com.alonzo.citeval.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDTO(
    Long id,
    @NotBlank @Email String email,
    String name,
    String role,
    boolean enabled
) {}
