package com.ai.interviewer.domain.dto;

public record CreateScheduledInterviewRequest(
        String title,
        String targetRole,
        String description,
        Integer totalQuestions,
        Integer durationMinutes,
        String scheduledStart,
        String scheduledEnd,
        String targetYear
) {
    public CreateScheduledInterviewRequest(
            String title,
            String targetRole,
            String description,
            Integer totalQuestions,
            Integer durationMinutes,
            String scheduledStart,
            String scheduledEnd
    ) {
        this(title, targetRole, description, totalQuestions, durationMinutes, scheduledStart, scheduledEnd, "ALL");
    }
}
