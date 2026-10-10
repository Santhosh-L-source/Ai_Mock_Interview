package com.ai.interviewer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SessionStatus status = SessionStatus.CREATED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 5;

    @Column(name = "proctor_warnings_count", nullable = false)
    private Integer proctorWarningsCount = 0;

    @Column(name = "malpractice_reason", columnDefinition = "TEXT")
    private String malpracticeReason;

    @Column(name = "is_malpractice")
    private Boolean isMalpractice = false;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "summary_feedback", columnDefinition = "TEXT")
    private String summaryFeedback;

    @Column(name = "session_token")
    private String sessionToken;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private InterviewAssignment assignment;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProctorViolationLog> violationLogs = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proctor_events_json", columnDefinition = "JSONB")
    private List<Map<String, Object>> proctorEvents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("questionNumber ASC")
    private List<InterviewTurn> turns = new ArrayList<>();

    public InterviewSession() {
    }

    public InterviewSession(Candidate candidate) {
        this(candidate, 5);
    }

    public InterviewSession(Candidate candidate, Integer totalQuestions) {
        this.candidate = candidate;
        this.totalQuestions = totalQuestions != null && totalQuestions > 0 ? totalQuestions : 5;
        this.status = SessionStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<InterviewTurn> getTurns() {
        return turns;
    }

    public void setTurns(List<InterviewTurn> turns) {
        this.turns = turns != null ? turns : new ArrayList<>();
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions != null && totalQuestions > 0 ? totalQuestions : 5;
    }

    public Integer getProctorWarningsCount() {
        return proctorWarningsCount != null ? proctorWarningsCount : 0;
    }

    public void setProctorWarningsCount(Integer proctorWarningsCount) {
        this.proctorWarningsCount = proctorWarningsCount;
    }

    public String getMalpracticeReason() {
        return malpracticeReason;
    }

    public void setMalpracticeReason(String malpracticeReason) {
        this.malpracticeReason = malpracticeReason;
    }

    public List<Map<String, Object>> getProctorEvents() {
        return proctorEvents != null ? proctorEvents : new ArrayList<>();
    }

    public void setProctorEvents(List<Map<String, Object>> proctorEvents) {
        this.proctorEvents = proctorEvents != null ? proctorEvents : new ArrayList<>();
    }

    public void addProctorEvent(Map<String, Object> event) {
        if (this.proctorEvents == null) {
            this.proctorEvents = new ArrayList<>();
        }
        this.proctorEvents.add(event);
    }

    public void addTurn(InterviewTurn turn) {
        turns.add(turn);
        turn.setSession(this);
    }

    public Boolean getIsMalpractice() {
        return isMalpractice != null ? isMalpractice : false;
    }

    public void setIsMalpractice(Boolean malpractice) {
        this.isMalpractice = malpractice;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Double technicalScore) {
        this.technicalScore = technicalScore;
    }

    public Double getCommunicationScore() {
        return communicationScore;
    }

    public void setCommunicationScore(Double communicationScore) {
        this.communicationScore = communicationScore;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getSummaryFeedback() {
        return summaryFeedback;
    }

    public void setSummaryFeedback(String summaryFeedback) {
        this.summaryFeedback = summaryFeedback;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public InterviewAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(InterviewAssignment assignment) {
        this.assignment = assignment;
    }

    public List<ProctorViolationLog> getViolationLogs() {
        return violationLogs;
    }

    public void setViolationLogs(List<ProctorViolationLog> violationLogs) {
        this.violationLogs = violationLogs;
    }

    public void addViolationLog(ProctorViolationLog log) {
        this.violationLogs.add(log);
        log.setSession(this);
    }
}
