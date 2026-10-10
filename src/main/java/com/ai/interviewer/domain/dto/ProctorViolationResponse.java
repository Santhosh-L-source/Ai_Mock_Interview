package com.ai.interviewer.domain.dto;

import com.ai.interviewer.domain.entity.SessionStatus;
import java.util.UUID;

public record ProctorViolationResponse(
    UUID sessionId,
    Integer totalWarnings,
    SessionStatus sessionStatus,
    Boolean disqualified,
    String message
) {}
