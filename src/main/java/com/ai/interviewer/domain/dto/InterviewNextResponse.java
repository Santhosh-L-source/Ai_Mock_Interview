package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record InterviewNextResponse(
    UUID sessionId,
    int answeredQuestionNumber,
    boolean isSessionCompleted,
    TurnEvaluation evaluation,
    GeneratedQuestionResponse nextQuestion,
    String ttsAudioBase64,
    FinalScorecardResponse finalSummary
) {}
