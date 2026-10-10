package com.ai.interviewer.domain.dto;

public record TtsResponse(
    String audioFormat,
    String audioBase64,
    Double durationSeconds,
    String text
) {}
