package com.ai.interviewer.domain.dto;

public record SubmitAnswerRequest(
    Integer questionNumber,
    String candidateTranscript,
    Boolean isTimeout,
    Integer timeTakenSeconds
) {
    public SubmitAnswerRequest(Integer questionNumber, String candidateTranscript) {
        this(questionNumber, candidateTranscript, false, null);
    }
}
