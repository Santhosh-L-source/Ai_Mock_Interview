package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record StartSessionRequest(
    UUID candidateId,
    Integer totalQuestions,
    UUID assignmentId,
    String sessionToken
) {
    public StartSessionRequest(UUID candidateId, Integer totalQuestions) {
        this(candidateId, totalQuestions, null, null);
    }
}
