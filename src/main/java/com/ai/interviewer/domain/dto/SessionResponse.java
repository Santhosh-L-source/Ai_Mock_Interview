package com.ai.interviewer.domain.dto;

import com.ai.interviewer.domain.entity.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID sessionId,
    UUID candidateId,
    String candidateName,
    SessionStatus status,
    Instant startedAt,
    InterviewRoadmap roadmap,
    GeneratedQuestionResponse currentQuestion,
    LiveKitTokenResponse livekit
) {}
