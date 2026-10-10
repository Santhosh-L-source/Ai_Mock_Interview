package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record AssignYearRequest(
        UUID interviewId,
        String targetYear
) {}
