package com.ai.interviewer.domain.dto;

public record PlannedTopic(
    int questionNumber,
    QuestionCategory category,
    String topic,
    String focusArea,
    String rationale,
    int timeLimitSeconds
) {
    public PlannedTopic(int questionNumber, QuestionCategory category, String topic, String focusArea, String rationale) {
        this(questionNumber, category, topic, focusArea, rationale,
                category == QuestionCategory.BEHAVIORAL ? 90 : 120);
    }
}
