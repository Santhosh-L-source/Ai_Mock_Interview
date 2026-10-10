package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.AssignCandidateRequest;
import com.ai.interviewer.domain.dto.AttendanceMarksRecord;
import com.ai.interviewer.domain.dto.CandidateAssignmentRecord;
import com.ai.interviewer.domain.dto.CreateStudentRequest;
import com.ai.interviewer.domain.dto.StudentResponse;
import com.ai.interviewer.domain.entity.AssignmentStatus;
import com.ai.interviewer.domain.entity.Candidate;
import com.ai.interviewer.domain.entity.InterviewAssignment;
import com.ai.interviewer.domain.entity.ScheduledInterview;
import com.ai.interviewer.repository.CandidateRepository;
import com.ai.interviewer.repository.InterviewAssignmentRepository;
import com.ai.interviewer.repository.InterviewSessionRepository;
import com.ai.interviewer.repository.ProctorViolationLogRepository;
import com.ai.interviewer.repository.ScheduledInterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminSchedulingServiceTest {

    private ScheduledInterviewRepository scheduledInterviewRepository;
    private InterviewAssignmentRepository assignmentRepository;
    private CandidateRepository candidateRepository;
    private InterviewSessionRepository sessionRepository;
    private ProctorViolationLogRepository violationLogRepository;
    private EmailService emailService;
    private AdminSchedulingService adminSchedulingService;

    @BeforeEach
    void setUp() {
        scheduledInterviewRepository = Mockito.mock(ScheduledInterviewRepository.class);
        assignmentRepository = Mockito.mock(InterviewAssignmentRepository.class);
        candidateRepository = Mockito.mock(CandidateRepository.class);
        sessionRepository = Mockito.mock(InterviewSessionRepository.class);
        violationLogRepository = Mockito.mock(ProctorViolationLogRepository.class);
        emailService = Mockito.mock(EmailService.class);

        adminSchedulingService = new AdminSchedulingService(
                scheduledInterviewRepository,
                assignmentRepository,
                candidateRepository,
                sessionRepository,
                violationLogRepository,
                emailService
        );
    }

    @Test
    @DisplayName("Should create student record with roll number eg: 24AM119")
    void testCreateStudentPermanentlyWithRollNo() {
        CreateStudentRequest request = new CreateStudentRequest(
                "Jane Doe",
                "jane.doe@college.edu",
                "24AM119",
                "SecurePass123",
                "2nd Year",
                "Artificial Intelligence & Machine Learning"
        );

        when(candidateRepository.findByEmail("jane.doe@college.edu")).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            return c;
        });

        StudentResponse response = adminSchedulingService.createStudentPermanently(request);

        assertNotNull(response);
        assertEquals("Jane Doe", response.name());
        assertEquals("jane.doe@college.edu", response.email());
        assertEquals("24AM119", response.rollNo());
        assertEquals("2nd Year", response.studentYear());
        assertEquals("Artificial Intelligence & Machine Learning", response.department());

        verify(candidateRepository).save(argThat(c ->
                "24AM119".equals(c.getRollNo()) &&
                "jane.doe@college.edu".equals(c.getEmail()) &&
                "Jane Doe".equals(c.getName())
        ));
    }

    @Test
    @DisplayName("Should assign candidate with roll number and populate attendance record")
    void testAssignCandidateAndGetAttendanceWithRollNo() {
        UUID interviewId = UUID.randomUUID();
        ScheduledInterview scheduledInterview = new ScheduledInterview("Drive 2026", "Developer", "Desc", 5, 15, Instant.now(), null);
        scheduledInterview.setId(interviewId);

        when(scheduledInterviewRepository.findById(interviewId)).thenReturn(Optional.of(scheduledInterview));
        when(candidateRepository.findByEmail("student@campus.edu")).thenReturn(Optional.empty());

        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(assignmentRepository.findByScheduledInterviewIdAndCandidateEmail(eq(interviewId), eq("student@campus.edu")))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(InterviewAssignment.class))).thenAnswer(invocation -> {
            InterviewAssignment a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AssignCandidateRequest assignRequest = new AssignCandidateRequest(
                interviewId,
                "Alex Morgan",
                "student@campus.edu",
                "24AM119",
                "Temp@123",
                "2nd Year",
                "Computer Science",
                true
        );

        CandidateAssignmentRecord assignRecord = adminSchedulingService.assignCandidate(assignRequest);
        assertNotNull(assignRecord);

        // Verify attendance record includes roll number
        Candidate student = new Candidate("Alex Morgan", "student@campus.edu", null, "resume", List.of());
        student.setId(UUID.randomUUID());
        student.setRollNo("24AM119");

        InterviewAssignment assignment = new InterviewAssignment(scheduledInterview, student, "student@campus.edu");
        assignment.setId(UUID.randomUUID());
        assignment.setAttemptStatus(AssignmentStatus.NOT_STARTED);

        when(assignmentRepository.findByScheduledInterviewIdWithCandidateAndSession(interviewId))
                .thenReturn(List.of(assignment));

        List<AttendanceMarksRecord> attendance = adminSchedulingService.getInterviewAttendance(interviewId);
        assertNotNull(attendance);
        assertEquals(1, attendance.size());
        assertEquals("Alex Morgan", attendance.get(0).candidateName());
        assertEquals("student@campus.edu", attendance.get(0).candidateEmail());
        assertEquals("24AM119", attendance.get(0).candidateRollNo());
    }
}
