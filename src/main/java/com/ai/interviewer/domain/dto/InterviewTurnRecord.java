package com.ai.interviewer.domain.dto;

import com.ai.interviewer.domain.entity.InterviewTurn;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InterviewTurnRecord(
    UUID id,
    UUID sessionId,
    Integer questionNumber,
    String questionText,
    String candidateAnswerTranscript,
    Double technicalScore,
    Double communicationScore,
    Map<String, Object> feedbackJson,
    Instant createdAt
) {
    public static InterviewTurnRecord fromEntity(InterviewTurn turn) {
        if (turn == null) return null;
        return new InterviewTurnRecord(
            turn.getId(),
            turn.getSession() != null ? turn.getSession().getId() : null,
            turn.getQuestionNumber(),
            turn.getQuestionText(),
            turn.getCandidateAnswerTranscript(),
            turn.getTechnicalScore(),
            turn.getCommunicationScore(),
            turn.getFeedbackJson(),
            turn.getCreatedAt()
        );
    }
}
