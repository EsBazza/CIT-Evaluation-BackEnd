package com.alonzo.citeval.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "professors")
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "role")
    private String role;

    @Column(name = "department", insertable = false, updatable = false)
    private String legacyDepartment;

    @Column(name = "assigned_sections")
    private String assignedSections;

    private boolean isActive = true;

    // =========================================
    //         GETTERS AND SETTERS
    // =========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() {
        if (role != null && !role.isBlank()) {
            return role;
        }
        return legacyDepartment;
    }
    public void setRole(String role) { this.role = role; }

    public String getAssignedSections() { return assignedSections; }
    public void setAssignedSections(String assignedSections) { this.assignedSections = assignedSections; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
