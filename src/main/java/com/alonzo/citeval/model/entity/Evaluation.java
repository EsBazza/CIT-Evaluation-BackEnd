package com.alonzo.citeval.model.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference; // Required import
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String facultyEmail;
    private String section;
    private double rating;
    private String studentNumber;
    @Column(name = "student_email", columnDefinition = "citext")
    private String studentEmail;

    @Column(columnDefinition = "TEXT")
    private String ciphertext;

    @Column(columnDefinition = "TEXT")
    private String studentPublicKey;

    private String iv;

    // ✅ FIX: Added @JsonManagedReference to allow scores to be serialized
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<EvaluationScore> scores = new ArrayList<>();

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFacultyEmail() { return facultyEmail; }
    public void setFacultyEmail(String facultyEmail) { this.facultyEmail = facultyEmail; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public String getStudentPublicKey() { return studentPublicKey; }
    public void setStudentPublicKey(String studentPublicKey) { this.studentPublicKey = studentPublicKey; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public List<EvaluationScore> getScores() { return scores; }
    public void setScores(List<EvaluationScore> scores) { this.scores = scores; }
}
