package com.ai.interviewer.domain.dto;

public record LiveKitTokenResponse(
    String roomName,
    String token,
    String serverUrl
) {}
