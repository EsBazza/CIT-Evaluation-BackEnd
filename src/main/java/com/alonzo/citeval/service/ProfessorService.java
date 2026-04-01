package com.alonzo.citeval.service;

import com.alonzo.citeval.model.entity.Professor;
import com.alonzo.citeval.repository.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    public List<Professor> getPublicProfessors(String section) {
        List<Professor> activeProfessors = professorRepository.findByIsActiveTrue();
        if (!StringUtils.hasText(section)) {
            return activeProfessors;
        }

        String normalizedSection = normalizeToken(section);
        return activeProfessors.stream()
                .filter(professor -> teachesSection(professor, normalizedSection))
                .toList();
    }

    public boolean isFacultyAllowedForSection(String facultyEmail, String section) {
        if (!StringUtils.hasText(facultyEmail) || !StringUtils.hasText(section)) {
            return false;
        }

        String normalizedSection = normalizeToken(section);

        return professorRepository.findByEmailIgnoreCase(facultyEmail.trim())
                .filter(Professor::isActive)
                .filter(professor -> teachesSection(professor, normalizedSection))
                .isPresent();
    }

    private boolean teachesSection(Professor professor, String normalizedSection) {
        if (!StringUtils.hasText(professor.getAssignedSections())) {
            return false;
        }

        return Arrays.stream(professor.getAssignedSections().split(","))
                .map(this::normalizeToken)
                .anyMatch(normalizedSection::equals);
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
