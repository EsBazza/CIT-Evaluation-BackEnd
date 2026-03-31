package com.alonzo.citeval.controller;

import com.alonzo.citeval.repository.CriterionRepository;
import com.alonzo.citeval.model.entity.Criterion;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api") // Changed to base /api to allow both admin and public paths
@CrossOrigin(origins = "http://localhost:5173")
public class CriterionController {

    private final CriterionRepository criterionRepository;

    public CriterionController(CriterionRepository criterionRepository) {
        this.criterionRepository = criterionRepository;
    }

    // --- PUBLIC ENDPOINTS (Fixes the student form 404) ---
    @GetMapping("/public/criteria")
    public List<Criterion> getPublicCriteria() {
        return criterionRepository.findAll();
    }

    // --- ADMIN ENDPOINTS (Matches your adminApi.js) ---
    @GetMapping("/admin/criteria")
    public List<Criterion> getAll() {
        return criterionRepository.findAll();
    }

    @PostMapping("/admin/criteria")
    public Criterion create(@RequestBody Criterion criterion) {
        return criterionRepository.save(criterion);
    }

    @PutMapping("/admin/criteria/{id}")
    public Criterion update(@PathVariable Long id, @RequestBody Criterion criterion) {
        criterion.setId(id);
        return criterionRepository.save(criterion);
    }

    @DeleteMapping("/admin/criteria/{id}")
    public void delete(@PathVariable Long id) {
        criterionRepository.deleteById(id);
    }
}
