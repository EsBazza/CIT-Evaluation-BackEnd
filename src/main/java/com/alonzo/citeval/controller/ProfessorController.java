package com.alonzo.citeval.controller;

import com.alonzo.citeval.repository.ProfessorRepository;
import com.alonzo.citeval.model.entity.Professor;
import com.alonzo.citeval.service.ProfessorService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api") // Base mapping changed to /api
@CrossOrigin(origins = "http://localhost:5173")
public class ProfessorController {

    private final ProfessorRepository professorRepository;
    private final ProfessorService professorService;

    public ProfessorController(ProfessorRepository professorRepository, ProfessorService professorService) {
        this.professorRepository = professorRepository;
        this.professorService = professorService;
    }

    // --- PUBLIC ENDPOINTS (Fixes the 404s) ---

    @GetMapping("/public/professors")
    public List<Professor> getPublicProfessors(@RequestParam(required = false) String section) {
        return professorService.getPublicProfessors(section);
    }

    // --- ADMIN ENDPOINTS (Matches your adminApi.js) ---

    @GetMapping("/admin/professors")
    public List<Professor> getAllForAdmin() {
        return professorRepository.findAll();
    }

    @PostMapping("/admin/professors")
    public Professor create(@RequestBody Professor professor) {
        // This will save the data to the DB
        return professorRepository.save(professor);
    }

    @PutMapping("/admin/professors/{id}")
    public Professor update(@PathVariable Long id, @RequestBody Professor professor) {
        professor.setId(id);
        return professorRepository.save(professor);
    }

    @DeleteMapping("/admin/professors/{id}")
    public void delete(@PathVariable Long id) {
        professorRepository.deleteById(id);
    }
}
