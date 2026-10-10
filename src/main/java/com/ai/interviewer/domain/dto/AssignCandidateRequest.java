package com.ai.interviewer.domain.dto;

import java.util.UUID;

public record AssignCandidateRequest(
        UUID interviewId,
        String candidateName,
        String candidateEmail,
        String rollNo,
        String temporaryPassword,
        String studentYear,
        String department,
        Boolean isPermanent
) {
    public AssignCandidateRequest(
            UUID interviewId,
            String candidateName,
            String candidateEmail,
            String temporaryPassword
    ) {
        this(interviewId, candidateName, candidateEmail, null, temporaryPassword, "3rd Year", "Computer Science & Engineering", true);
    }

    public AssignCandidateRequest(
            UUID interviewId,
            String candidateName,
            String candidateEmail,
            String temporaryPassword,
            String studentYear,
            String department,
            Boolean isPermanent
    ) {
        this(interviewId, candidateName, candidateEmail, null, temporaryPassword, studentYear, department, isPermanent);
    }
}
