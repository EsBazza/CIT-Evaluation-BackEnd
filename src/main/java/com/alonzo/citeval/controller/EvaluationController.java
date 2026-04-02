package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.dto.EvaluationRequestDTO;
import com.alonzo.citeval.model.dto.EvaluationResponseDTO;
import com.alonzo.citeval.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

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
    public String decrypt(@PathVariable Long id,
                          @RequestParam(required = false) String facultyEmail,
                          Authentication authentication) {
        EvaluationResponseDTO eval = evaluationService.getEvaluationById(id);

        if (!isAdmin(authentication)) {
            if (facultyEmail == null || facultyEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty email is required for this request");
            }

            String requestedEmail = facultyEmail.trim().toLowerCase(Locale.ROOT);
            String evaluationEmail = eval.facultyEmail() == null ? "" : eval.facultyEmail().trim().toLowerCase(Locale.ROOT);
            if (!requestedEmail.equals(evaluationEmail)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only decrypt your own evaluations");
            }
        }

        try {
            return evaluationService.decryptFeedback(eval.ciphertext(), eval.studentPublicKey(), eval.iv());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Decryption failed");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }
}
