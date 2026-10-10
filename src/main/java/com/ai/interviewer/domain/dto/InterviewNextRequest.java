package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record InterviewNextRequest(
    UUID sessionId,
    int questionNumber,
    String candidateTranscript,
    Boolean isTimeout,
    Integer timeTakenSeconds
) {
    public InterviewNextRequest(UUID sessionId, int questionNumber, String candidateTranscript) {
        this(sessionId, questionNumber, candidateTranscript, false, null);
    }
}
