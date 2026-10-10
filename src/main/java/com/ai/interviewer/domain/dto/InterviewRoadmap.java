package com.ai.interviewer.domain.dto;

import java.util.List;

public record InterviewRoadmap(
    String candidateName,
    int totalQuestions,
    List<PlannedTopic> plan,
    String summary
) {}
