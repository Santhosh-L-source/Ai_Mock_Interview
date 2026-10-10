package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record TurnResponse(
    UUID turnId,
    Integer questionNumber,
    String questionText,
    String candidateTranscript,
    TurnEvaluation evaluation,
    GeneratedQuestionResponse nextQuestion,
    boolean isCompleted
) {}
