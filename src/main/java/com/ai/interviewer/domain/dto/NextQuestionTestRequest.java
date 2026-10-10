package com.ai.interviewer.domain.dto;

import java.util.List;
import java.util.UUID;

public record NextQuestionTestRequest(
    UUID candidateId,
    ParsedResume resume,
    PlannedTopic plannedTopic,
    int questionNumber,
    List<TurnContext> previousTurns
) {}
