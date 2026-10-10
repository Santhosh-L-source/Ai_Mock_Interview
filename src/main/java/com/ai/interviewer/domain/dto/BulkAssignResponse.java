package com.ai.interviewer.domain.dto;

public record BulkAssignResponse(
        int assignedCount,
        String message
) {}
