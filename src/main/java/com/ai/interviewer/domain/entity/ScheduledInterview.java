package com.ai.interviewer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scheduled_interviews")
public class ScheduledInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 7;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 15;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart = Instant.now();

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, ARCHIVED, CLOSED

    @Column(name = "target_year", length = 50)
    private String targetYear = "ALL";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "scheduledInterview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewAssignment> assignments = new ArrayList<>();

    public ScheduledInterview() {
    }

    public ScheduledInterview(String title, String targetRole, String description, Integer totalQuestions, Integer durationMinutes, Instant scheduledStart, Instant scheduledEnd) {
        this.title = title;
        this.targetRole = targetRole;
        this.description = description;
        this.totalQuestions = totalQuestions != null && totalQuestions > 0 ? totalQuestions : 7;
        this.durationMinutes = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 15;
        this.scheduledStart = scheduledStart != null ? scheduledStart : Instant.now();
        this.scheduledEnd = scheduledEnd;
        this.status = "ACTIVE";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Instant getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(Instant scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(String targetYear) {
        this.targetYear = targetYear;
    }

    public List<InterviewAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<InterviewAssignment> assignments) {
        this.assignments = assignments;
    }
}
