package com.alonzo.citeval.model.dto;

public record EvaluationScoreDTO(
    Long id,
    Long criterionId,
    String criterionName,
    int score
) {}
