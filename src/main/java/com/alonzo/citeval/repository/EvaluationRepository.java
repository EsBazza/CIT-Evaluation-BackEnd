package com.alonzo.citeval.repository;

import com.alonzo.citeval.model.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;



@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    boolean existsByStudentNumberAndFacultyEmailAndSection(String studentNumber, String facultyEmail, String section);

    @Query("SELECT e FROM Evaluation e LEFT JOIN FETCH e.scores s LEFT JOIN FETCH s.criterion")
    List<Evaluation> findAllWithScores();

    @Query("SELECT e FROM Evaluation e LEFT JOIN FETCH e.scores s LEFT JOIN FETCH s.criterion WHERE e.facultyEmail = :facultyEmail")
    List<Evaluation> findByFacultyEmailWithScores(@Param("facultyEmail") String facultyEmail);

    @Query("SELECT e FROM Evaluation e LEFT JOIN FETCH e.scores s LEFT JOIN FETCH s.criterion WHERE e.id = :id")
    Optional<Evaluation> findWithScoresById(@Param("id") Long id);

    List<Evaluation> findByFacultyEmail(String facultyEmail);
}
