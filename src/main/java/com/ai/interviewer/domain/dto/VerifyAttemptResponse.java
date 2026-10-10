package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record VerifyAttemptResponse(
        boolean allowed,
        String reason,
        UUID assignmentId,
        UUID interviewId,
        String interviewTitle,
        String attemptStatus,
        Integer durationMinutes,
        Integer totalQuestions,
        UUID existingSessionId,
        String sessionToken,
        boolean isResumption
) {}
