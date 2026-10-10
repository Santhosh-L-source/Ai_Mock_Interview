package com.ai.interviewer.domain.dto;

public record ResetPasswordResponse(
        boolean success,
        String message
) {}
