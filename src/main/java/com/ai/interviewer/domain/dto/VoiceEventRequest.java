package com.ai.interviewer.domain.dto;

public record VoiceEventRequest(
    String eventType, // NEXT_BUTTON_CLICKED, TIMER_EXPIRED, CANDIDATE_SPEAKING, CANDIDATE_SILENT
    Integer questionNumber,
    String candidateTranscript,
    Boolean isTimeout,
    Integer timeTakenSeconds
) {
    public VoiceEventRequest(String eventType, Integer questionNumber, String candidateTranscript) {
        this(eventType, questionNumber, candidateTranscript, "TIMER_EXPIRED".equalsIgnoreCase(eventType), null);
    }
}
