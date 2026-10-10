package com.ai.interviewer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"interview_id", "candidate_id"}),
        @UniqueConstraint(columnNames = {"interview_id", "candidate_email"})
})
public class InterviewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private ScheduledInterview scheduledInterview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "candidate_email", nullable = false)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status", nullable = false, length = 50)
    private AssignmentStatus attemptStatus = AssignmentStatus.NOT_STARTED;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "attempted_at")
    private Instant attemptedAt;

    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private InterviewSession session;

    public InterviewAssignment() {
    }

    public InterviewAssignment(ScheduledInterview scheduledInterview, Candidate candidate, String candidateEmail) {
        this.scheduledInterview = scheduledInterview;
        this.candidate = candidate;
        this.candidateEmail = candidateEmail;
        this.attemptStatus = AssignmentStatus.NOT_STARTED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ScheduledInterview getScheduledInterview() {
        return scheduledInterview;
    }

    public void setScheduledInterview(ScheduledInterview scheduledInterview) {
        this.scheduledInterview = scheduledInterview;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public AssignmentStatus getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(AssignmentStatus attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public InterviewSession getSession() {
        return session;
    }

    public void setSession(InterviewSession session) {
        this.session = session;
    }
}
