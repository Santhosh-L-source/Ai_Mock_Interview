package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record VoiceEventResponse(
    String eventType,
    UUID sessionId,
    int currentQuestionNumber,
    boolean isSessionCompleted,
    GeneratedQuestionResponse nextQuestion,
    TurnEvaluation evaluation,
    String ttsAudioBase64,
    String statusMessage
) {}
