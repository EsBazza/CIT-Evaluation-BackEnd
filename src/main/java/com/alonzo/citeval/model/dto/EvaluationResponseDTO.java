package com.alonzo.citeval.model.dto;

import java.util.List;

public record EvaluationResponseDTO(
    Long id,
    String studentNumber,
    String studentEmail,
    String facultyEmail,
    String section,
    double rating,
    String ciphertext,
    String studentPublicKey,
    String iv,
    List<EvaluationScoreDTO> scores
) {}
