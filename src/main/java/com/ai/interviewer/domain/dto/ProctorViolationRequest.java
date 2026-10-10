package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record ProctorViolationRequest(
    String violationType,     // TAB_SWITCH, MOBILE_PHONE_DETECTED, FULLSCREEN_EXIT, MULTIPLE_FACES
    String details,
    Integer warningNumber,
    Boolean isMalpractice,
    String timestamp
) {}
