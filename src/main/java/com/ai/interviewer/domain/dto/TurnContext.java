package com.ai.interviewer.domain.dto;

import java.util.List;
import java.util.UUID;

public record TurnContext(
    int questionNumber,
    String questionText,
    String candidateTranscript,
    Double technicalScore,
    Double communicationScore
) {}
