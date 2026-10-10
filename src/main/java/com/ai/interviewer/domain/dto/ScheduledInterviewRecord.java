package com.ai.interviewer.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record ScheduledInterviewRecord(
        UUID id,
        String title,
        String targetRole,
        String description,
        Integer totalQuestions,
        Integer durationMinutes,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status,
        String targetYear,
        long totalAssigned,
        long completedCount,
        long malpracticeCount,
        Double averageScore,
        Instant createdAt
) {
    public ScheduledInterviewRecord(
            UUID id,
            String title,
            String targetRole,
            String description,
            Integer totalQuestions,
            Integer durationMinutes,
            Instant scheduledStart,
            Instant scheduledEnd,
            String status,
            long totalAssigned,
            long completedCount,
            long malpracticeCount,
            Double averageScore,
            Instant createdAt
    ) {
        this(id, title, targetRole, description, totalQuestions, durationMinutes, scheduledStart, scheduledEnd, status, "ALL", totalAssigned, completedCount, malpracticeCount, averageScore, createdAt);
    }
}
