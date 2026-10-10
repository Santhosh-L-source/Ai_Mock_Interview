package com.ai.interviewer.domain.dto;

public record ResetPasswordRequest(
        String email,
        String newPassword
) {}
