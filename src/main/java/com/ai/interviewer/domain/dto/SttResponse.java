package com.ai.interviewer.domain.dto;

public record SttResponse(
    String transcript,
    Double confidence,
    boolean isFinal,
    String language
) {}
