package com.ai.interviewer.domain.dto;

import java.util.List;

public record ParsedResume(
    String candidateName,
    List<String> technicalSkills,
    List<String> domainAreas,
    Integer experienceYears,
    String summary,
    List<String> keyProjects
) {}
