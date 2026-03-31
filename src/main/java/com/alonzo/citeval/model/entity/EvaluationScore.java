package com.alonzo.citeval.model.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference; // Required import

@Entity
@Table(name = "evaluation_scores")
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int score;

    @ManyToOne
    @JoinColumn(name = "criterion_id")
    private Criterion criterion;

    // ✅ FIX: Added @JsonBackReference to stop the infinite loop
    @ManyToOne
    @JoinColumn(name = "evaluation_id")
    @JsonBackReference
    private Evaluation evaluation;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Criterion getCriterion() { return criterion; }
    public void setCriterion(Criterion criterion) { this.criterion = criterion; }

    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
}
