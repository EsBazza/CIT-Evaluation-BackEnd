package com.alonzo.citeval.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record EvaluationRequestDTO(
    @NotBlank
    @Pattern(regexp = "^\\d{6,20}$", message = "studentNumber must be 6-20 digits")
    String studentNumber,
    @NotBlank @Email String studentEmail,
    @NotBlank String facultyEmail,
    @NotBlank
    @Pattern(regexp = "^[0-9]+-[A-Za-z0-9]+$", message = "section must be in YEAR-SECTION format, e.g. 2-A")
    String section,
    @NotBlank String ciphertext,
    @NotBlank String studentPublicKey,
    @NotBlank String iv,
    @Valid List<ScoreRequestDTO> scores
) {}
