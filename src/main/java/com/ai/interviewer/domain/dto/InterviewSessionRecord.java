package com.ai.interviewer.domain.dto;

import com.ai.interviewer.domain.entity.InterviewSession;
import com.ai.interviewer.domain.entity.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewSessionRecord(
    UUID id,
    UUID candidateId,
    String candidateName,
    SessionStatus status,
    Instant startedAt,
    Instant endedAt,
    Instant createdAt,
    Integer proctorWarningsCount,
    String malpracticeReason,
    List<java.util.Map<String, Object>> proctorEvents,
    List<InterviewTurnRecord> turns
) {
    public static InterviewSessionRecord fromEntity(InterviewSession session) {
        if (session == null) return null;
        List<InterviewTurnRecord> turnRecords = session.getTurns() != null
            ? session.getTurns().stream().map(InterviewTurnRecord::fromEntity).toList()
            : List.of();

        return new InterviewSessionRecord(
            session.getId(),
            session.getCandidate() != null ? session.getCandidate().getId() : null,
            session.getCandidate() != null ? session.getCandidate().getName() : null,
            session.getStatus(),
            session.getStartedAt(),
            session.getEndedAt(),
            session.getCreatedAt(),
            session.getProctorWarningsCount(),
            session.getMalpracticeReason(),
            session.getProctorEvents(),
            turnRecords
        );
    }
}
