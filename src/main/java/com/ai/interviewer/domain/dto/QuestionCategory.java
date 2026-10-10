package com.ai.interviewer.domain.dto;

public enum QuestionCategory {
    RESUME_SKILL("Resume Technical Skills (50%)"),
    DSA_BASIC("Fundamental Data Structures & Algorithms (30%)"),
    BEHAVIORAL("College Projects & Behavioral STAR (20%)");

    private final String description;

    QuestionCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
