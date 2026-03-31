package com.alonzo.citeval.model.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "criteria")
public class Criterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // ✅ FIX: Added relationship with CascadeType.REMOVE
    // This allows the deletion of a criterion even if it has linked scores.
    @OneToMany(mappedBy = "criterion", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore // We use @JsonIgnore so we don't send all scores back when fetching the list
    private List<EvaluationScore> scores;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<EvaluationScore> getScores() { return scores; }
    public void setScores(List<EvaluationScore> scores) { this.scores = scores; }
}
