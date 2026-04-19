package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.entity.Evaluation;
import com.alonzo.citeval.model.entity.Professor;
import com.alonzo.citeval.repository.EvaluationRepository;
import com.alonzo.citeval.repository.ProfessorRepository;
import com.alonzo.citeval.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;
    private final EvaluationRepository evaluationRepository;
    private final ProfessorRepository professorRepository;

    public ExportController(ExportService exportService,
                            EvaluationRepository evaluationRepository,
                            ProfessorRepository professorRepository) {
        this.exportService = exportService;
        this.evaluationRepository = evaluationRepository;
        this.professorRepository = professorRepository;
    }

    @GetMapping("/all/csv")
    public ResponseEntity<byte[]> exportAllCsv(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can export all data");
        }
        byte[] csv = exportService.exportAllToCsv();
        return createResponseEntity(csv, "all_evaluations.csv", "text/csv");
    }

    @GetMapping("/all/pdf")
    public ResponseEntity<byte[]> exportAllPdf(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can export all data");
        }
        byte[] pdf = exportService.exportAllToPdf();
        return createResponseEntity(pdf, "all_evaluations.pdf", "application/pdf");
    }

    @GetMapping("/evaluation/{id}/csv")
    public ResponseEntity<byte[]> exportEvaluationCsv(Authentication authentication, @PathVariable Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));

        if (!isAdmin(authentication) && !isOwner(authentication, eval.getFacultyEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to export this evaluation");
        }

        byte[] csv = exportService.exportEvaluationToCsv(id);
        return createResponseEntity(csv, "evaluation_" + id + ".csv", "text/csv");
    }

    @GetMapping("/evaluation/{id}/pdf")
    public ResponseEntity<byte[]> exportEvaluationPdf(Authentication authentication, @PathVariable Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));

        if (!isAdmin(authentication) && !isOwner(authentication, eval.getFacultyEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to export this evaluation");
        }

        byte[] pdf = exportService.exportEvaluationToPdf(id);
        return createResponseEntity(pdf, "evaluation_" + id + ".pdf", "application/pdf");
    }

    @GetMapping("/professor/{id}/csv")
    public ResponseEntity<byte[]> exportProfessorCsv(Authentication authentication, @PathVariable Long id) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor not found"));

        if (!isAdmin(authentication) && !isOwner(authentication, professor.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to export this professor's data");
        }

        byte[] csv = exportService.exportProfessorToCsv(id);
        return createResponseEntity(csv, "professor_" + id + "_evaluations.csv", "text/csv");
    }

    @GetMapping("/professor/{id}/pdf")
    public ResponseEntity<byte[]> exportProfessorPdf(Authentication authentication, @PathVariable Long id) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor not found"));

        if (!isAdmin(authentication) && !isOwner(authentication, professor.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to export this professor's data");
        }

        byte[] pdf = exportService.exportProfessorToPdf(id);
        return createResponseEntity(pdf, "professor_" + id + "_evaluations.pdf", "application/pdf");
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isOwner(Authentication authentication, String ownerEmail) {
        if (authentication == null || ownerEmail == null) return false;
        String userEmail = authentication.getName(); // In JWT, name is often the email
        return ownerEmail.equalsIgnoreCase(userEmail);
    }

    private ResponseEntity<byte[]> createResponseEntity(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
