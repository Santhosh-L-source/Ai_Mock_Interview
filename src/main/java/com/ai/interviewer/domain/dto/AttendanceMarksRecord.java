package com.ai.interviewer.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttendanceMarksRecord(
        UUID assignmentId,
        UUID sessionId,
        UUID candidateId,
        String candidateName,
        String candidateEmail,
        String candidateRollNo,
        String attemptStatus,
        Instant assignedAt,
        Instant startedAt,
        Instant endedAt,
        Double overallScore,
        Double technicalScore,
        Double communicationScore,
        String verdict,
        Boolean isMalpractice,
        Integer proctorWarningsCount,
        String malpracticeReason,
        List<String> violationDetails
) {}
