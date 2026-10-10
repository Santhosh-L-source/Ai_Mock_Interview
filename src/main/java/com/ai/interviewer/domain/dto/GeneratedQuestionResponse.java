package com.ai.interviewer.domain.dto;

public record GeneratedQuestionResponse(
    int questionNumber,
    QuestionCategory category,
    String topic,
    String questionText,
    String contextRationale,
    String expectedKeyPoints,
    Integer timeLimitSeconds
) {
    public GeneratedQuestionResponse(
            int questionNumber,
            QuestionCategory category,
            String topic,
            String questionText,
            String contextRationale,
            String expectedKeyPoints) {
        this(questionNumber, category, topic, questionText, contextRationale, expectedKeyPoints,
                category == QuestionCategory.BEHAVIORAL ? 90 : 120);
    }
}
