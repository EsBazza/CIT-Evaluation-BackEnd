package com.alonzo.citeval.repository;

import com.alonzo.citeval.model.entity.Evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;



@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    boolean existsByStudentNumberAndFacultyEmailAndSection(String studentNumber, String facultyEmail, String section);
    List<Evaluation> findByFacultyEmail(String facultyEmail);
}
