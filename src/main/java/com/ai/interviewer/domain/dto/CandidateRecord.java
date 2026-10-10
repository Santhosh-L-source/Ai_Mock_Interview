package com.ai.interviewer.domain.dto;

import com.ai.interviewer.domain.entity.Candidate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateRecord(
    UUID id,
    String name,
    String email,
    String rollNo,
    String authUserId,
    String resumeText,
    List<String> skills,
    Instant createdAt
) {
    public CandidateRecord(UUID id, String name, String resumeText, List<String> skills, Instant createdAt) {
        this(id, name, null, null, null, resumeText, skills, createdAt);
    }

    public CandidateRecord(UUID id, String name, String email, String authUserId, String resumeText, List<String> skills, Instant createdAt) {
        this(id, name, email, null, authUserId, resumeText, skills, createdAt);
    }

    public static CandidateRecord fromEntity(Candidate candidate) {
        if (candidate == null) return null;
        return new CandidateRecord(
            candidate.getId(),
            candidate.getName(),
            candidate.getEmail(),
            candidate.getRollNo(),
            candidate.getAuthUserId(),
            candidate.getResumeText(),
            candidate.getSkills(),
            candidate.getCreatedAt()
        );
    }
}
