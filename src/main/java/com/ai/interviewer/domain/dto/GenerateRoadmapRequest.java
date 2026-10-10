package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record GenerateRoadmapRequest(
    UUID candidateId,
    ParsedResume resume,
    Integer totalQuestions
) {}
