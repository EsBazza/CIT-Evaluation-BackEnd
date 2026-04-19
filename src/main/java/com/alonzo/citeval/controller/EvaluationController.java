package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.dto.EvaluationRequestDTO;
import com.alonzo.citeval.model.dto.EvaluationResponseDTO;
import com.alonzo.citeval.service.EvaluationService;
import com.alonzo.citeval.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final ExportService exportService;

    public EvaluationController(EvaluationService evaluationService, ExportService exportService) {
        this.evaluationService = evaluationService;
        this.exportService = exportService;
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
                          @AuthenticationPrincipal String principalName,
                          Authentication authentication) {
        EvaluationResponseDTO eval = evaluationService.getEvaluationById(id);
        assertFacultyOwnerOrAdmin(authentication, principalName, eval.facultyEmail());

        try {
            return evaluationService.decryptFeedback(eval.ciphertext(), eval.studentPublicKey(), eval.iv());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Decryption failed");
        }
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportAllCsv(Authentication authentication) {
        assertAdmin(authentication);
        byte[] csv = exportService.exportAllToCsv();
        return buildFileResponse(csv, "all_evaluations.csv", "text/csv");
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportAllPdf(Authentication authentication) {
        assertAdmin(authentication);
        byte[] pdf = exportService.exportAllToPdf();
        return buildFileResponse(pdf, "all_evaluations.pdf", "application/pdf");
    }

    @GetMapping("/export/faculty/csv")
    public ResponseEntity<byte[]> exportFacultyCsv(@RequestParam String facultyEmail,
                                                   @AuthenticationPrincipal String principalName,
                                                   Authentication authentication) {
        assertFacultyOwnerOrAdmin(authentication, principalName, facultyEmail);
        byte[] csv = exportService.exportFacultyToCsv(facultyEmail);
        return buildFileResponse(csv,
                "faculty_evaluations_" + sanitizeFileToken(facultyEmail) + ".csv",
                "text/csv");
    }

    @GetMapping("/export/faculty/pdf")
    public ResponseEntity<byte[]> exportFacultyPdf(@RequestParam String facultyEmail,
                                                   @AuthenticationPrincipal String principalName,
                                                   Authentication authentication) {
        assertFacultyOwnerOrAdmin(authentication, principalName, facultyEmail);
        byte[] pdf = exportService.exportFacultyToPdf(facultyEmail);
        return buildFileResponse(pdf,
                "faculty_evaluations_" + sanitizeFileToken(facultyEmail) + ".pdf",
                "application/pdf");
    }

    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportEvaluationCsv(@PathVariable Long id,
                                                      @AuthenticationPrincipal String principalName,
                                                      Authentication authentication) {
        EvaluationResponseDTO eval = evaluationService.getEvaluationById(id);
        assertFacultyOwnerOrAdmin(authentication, principalName, eval.facultyEmail());

        byte[] csv = exportService.exportEvaluationToCsv(id);
        return buildFileResponse(csv, "evaluation_" + id + ".csv", "text/csv");
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportEvaluationPdf(@PathVariable Long id,
                                                      @AuthenticationPrincipal String principalName,
                                                      Authentication authentication) {
        EvaluationResponseDTO eval = evaluationService.getEvaluationById(id);
        assertFacultyOwnerOrAdmin(authentication, principalName, eval.facultyEmail());

        byte[] pdf = exportService.exportEvaluationToPdf(id);
        return buildFileResponse(pdf, "evaluation_" + id + ".pdf", "application/pdf");
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    private void assertAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private void assertFacultyOwnerOrAdmin(Authentication authentication,
                                           String principalName,
                                           String targetFacultyEmail) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (isAdmin(authentication)) {
            return;
        }

        String caller = normalize(principalName != null ? principalName : authentication.getName());
        String target = normalize(targetFacultyEmail);

        if (caller.isEmpty() || target.isEmpty() || !caller.equals(target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own evaluations");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
