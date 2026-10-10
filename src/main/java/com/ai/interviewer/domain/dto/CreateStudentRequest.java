package com.ai.interviewer.domain.dto;

public record CreateStudentRequest(
        String name,
        String email,
        String rollNo,
        String password,
        String studentYear,
        String department
) {
    public CreateStudentRequest(String name, String email, String password, String studentYear, String department) {
        this(name, email, null, password, studentYear, department);
    }
}
