package com.alonzo.citeval.repository;

import com.alonzo.citeval.model.entity.Professor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
	List<Professor> findByIsActiveTrue();
	Optional<Professor> findByEmailIgnoreCase(String email);
}
