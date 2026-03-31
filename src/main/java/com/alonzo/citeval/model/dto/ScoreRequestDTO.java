package com.alonzo.citeval.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreRequestDTO(
    @NotNull Long criterionId,
    @NotNull Integer score
) {}
