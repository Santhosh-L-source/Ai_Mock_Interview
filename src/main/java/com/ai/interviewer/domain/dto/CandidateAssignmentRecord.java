package com.ai.interviewer.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record CandidateAssignmentRecord(
        UUID assignmentId,
        UUID interviewId,
        String interviewTitle,
        String targetRole,
        String description,
        Integer durationMinutes,
        Integer totalQuestions,
        String attemptStatus,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant attemptedAt,
        Double overallScore,
        Double technicalScore,
        Double communicationScore,
        String verdict,
        String targetYear,
        UUID sessionId,
        String sessionToken
) {
    public CandidateAssignmentRecord(
            UUID assignmentId,
            UUID interviewId,
            String interviewTitle,
            String targetRole,
            String description,
            Integer durationMinutes,
            Integer totalQuestions,
            String attemptStatus,
            Instant scheduledStart,
            Instant scheduledEnd,
            Instant attemptedAt,
            Double overallScore,
            String verdict,
            UUID sessionId,
            String sessionToken
    ) {
        this(assignmentId, interviewId, interviewTitle, targetRole, description, durationMinutes, totalQuestions, attemptStatus, scheduledStart, scheduledEnd, attemptedAt, overallScore, null, null, verdict, "ALL", sessionId, sessionToken);
    }
}
