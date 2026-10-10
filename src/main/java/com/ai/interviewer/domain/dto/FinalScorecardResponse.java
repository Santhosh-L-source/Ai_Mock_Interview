package com.ai.interviewer.domain.dto;

import java.util.List;
import java.util.UUID;

public record FinalScorecardResponse(
    UUID sessionId,
    String candidateName,
    Double overallScore,
    Double technicalAverage,
    Double communicationAverage,
    String overallVerdict,
    String detailedFeedback,
    List<TurnSummaryRecord> turnSummaries
) {}
