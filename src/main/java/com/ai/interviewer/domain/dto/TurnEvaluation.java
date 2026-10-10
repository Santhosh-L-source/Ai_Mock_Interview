package com.ai.interviewer.domain.dto;

import java.util.List;

public record TurnEvaluation(
    Double technicalScore,
    Double communicationScore,
    List<String> keyPointsCovered,
    List<String> keyPointsMissed,
    String strengths,
    String areasToImprove,
    String feedbackSummary,
    String suggestedImprovement
) {}
