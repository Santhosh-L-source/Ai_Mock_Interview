package com.ai.interviewer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proctor_violation_logs")
public class ProctorViolationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "violation_type", nullable = false, length = 100)
    private String violationType;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "warning_number", nullable = false)
    private Integer warningNumber;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private Instant loggedAt;

    public ProctorViolationLog() {
    }

    public ProctorViolationLog(InterviewSession session, String violationType, String details, Integer warningNumber) {
        this.session = session;
        this.violationType = violationType;
        this.details = details;
        this.warningNumber = warningNumber;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InterviewSession getSession() {
        return session;
    }

    public void setSession(InterviewSession session) {
        this.session = session;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Integer getWarningNumber() {
        return warningNumber;
    }

    public void setWarningNumber(Integer warningNumber) {
        this.warningNumber = warningNumber;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }
}
