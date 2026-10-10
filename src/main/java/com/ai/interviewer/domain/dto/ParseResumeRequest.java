package com.ai.interviewer.domain.dto;

import java.util.List;
import java.util.UUID;

public record ParseResumeRequest(
    String resumeText,
    String candidateName,
    String email,
    String authUserId
) {
    public ParseResumeRequest(String resumeText, String candidateName) {
        this(resumeText, candidateName, null, null);
    }
}
