package com.ai.interviewer.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String email,
        String rollNo,
        String studentYear,
        String department,
        String rawPassword,
        Instant createdAt
) {
    public StudentResponse(UUID id, String name, String email, String studentYear, String department, String rawPassword, Instant createdAt) {
        this(id, name, email, null, studentYear, department, rawPassword, createdAt);
    }
}
