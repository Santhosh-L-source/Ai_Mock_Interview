package com.ai.interviewer.domain.dto;

public record TurnSummaryRecord(
    Integer questionNumber,
    String questionText,
    String candidateTranscript,
    Double technicalScore,
    Double communicationScore,
    String feedbackSummary
) {}
