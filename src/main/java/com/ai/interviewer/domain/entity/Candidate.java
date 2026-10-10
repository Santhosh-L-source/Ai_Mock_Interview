package com.ai.interviewer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "auth_user_id")
    private String authUserId;

    @Column(name = "temporary_password_issued")
    private Boolean temporaryPasswordIssued = false;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    @Column(name = "student_year")
    private String studentYear = "3rd Year";

    @Column(name = "department")
    private String department = "Computer Science & Engineering";

    @Column(name = "roll_no", length = 50)
    private String rollNo;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "JSONB", nullable = false)
    private List<String> skills = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Candidate() {
    }

    public Candidate(String name, String resumeText, List<String> skills) {
        this.name = name;
        this.resumeText = resumeText;
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public Candidate(String name, String email, String authUserId, String resumeText, List<String> skills) {
        this.name = name;
        this.email = email;
        this.authUserId = authUserId;
        this.resumeText = resumeText;
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public Boolean getTemporaryPasswordIssued() {
        return temporaryPasswordIssued;
    }

    public void setTemporaryPasswordIssued(Boolean temporaryPasswordIssued) {
        this.temporaryPasswordIssued = temporaryPasswordIssued;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getStudentYear() {
        return studentYear;
    }

    public void setStudentYear(String studentYear) {
        this.studentYear = studentYear;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
