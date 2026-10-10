package com.ai.interviewer.domain.dto;

public record TtsRequest(
    String text,
    String voice,
    Double speed
) {
    public TtsRequest(String text) {
        this(text, "interviewer-default", 1.0);
    }
}
