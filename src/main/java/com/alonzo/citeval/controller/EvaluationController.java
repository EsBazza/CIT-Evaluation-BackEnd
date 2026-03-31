package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.dto.EvaluationRequestDTO;
import com.alonzo.citeval.model.dto.EvaluationResponseDTO;
import com.alonzo.citeval.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationResponseDTO submit(@Valid @RequestBody EvaluationRequestDTO req) {
        return evaluationService.submitEvaluation(req);
    }

    @GetMapping
    public List<EvaluationResponseDTO> getAll(@RequestParam(required = false) String facultyEmail) {
        return evaluationService.getAllEvaluations(facultyEmail);
    }

    @GetMapping("/{id}/decrypt")
    public String decrypt(@PathVariable Long id) {
        EvaluationResponseDTO eval = evaluationService.getEvaluationById(id);

        try {
            return evaluationService.decryptFeedback(eval.ciphertext(), eval.studentPublicKey(), eval.iv());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Decryption failed");
        }
    }
}
