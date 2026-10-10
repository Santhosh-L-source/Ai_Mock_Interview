package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.domain.entity.*;
import com.ai.interviewer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(AdminSchedulingService.class);

    private final ScheduledInterviewRepository scheduledInterviewRepository;
    private final InterviewAssignmentRepository assignmentRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewSessionRepository sessionRepository;
    private final ProctorViolationLogRepository violationLogRepository;
    private final EmailService emailService;

    public AdminSchedulingService(
            ScheduledInterviewRepository scheduledInterviewRepository,
            InterviewAssignmentRepository assignmentRepository,
            CandidateRepository candidateRepository,
            InterviewSessionRepository sessionRepository,
            ProctorViolationLogRepository violationLogRepository,
            EmailService emailService) {
        this.scheduledInterviewRepository = scheduledInterviewRepository;
        this.assignmentRepository = assignmentRepository;
        this.candidateRepository = candidateRepository;
        this.sessionRepository = sessionRepository;
        this.violationLogRepository = violationLogRepository;
        this.emailService = emailService;
    }

    /**
     * Admin creates a new scheduled interview session/campaign.
     */
    @Transactional
    public ScheduledInterviewRecord createScheduledInterview(CreateScheduledInterviewRequest request) {
        log.info("Creating scheduled interview: {}", request.title());

        Instant start = Instant.now();
        if (request.scheduledStart() != null && !request.scheduledStart().isBlank()) {
            try {
                start = Instant.parse(request.scheduledStart());
            } catch (Exception ignored) {}
        }

        Instant end = null;
        if (request.scheduledEnd() != null && !request.scheduledEnd().isBlank()) {
            try {
                end = Instant.parse(request.scheduledEnd());
            } catch (Exception ignored) {}
        }

        ScheduledInterview interview = new ScheduledInterview(
                request.title(),
                request.targetRole() != null ? request.targetRole() : "Software Engineer",
                request.description(),
                request.totalQuestions(),
                request.durationMinutes(),
                start,
                end
        );

        if (request.targetYear() != null && !request.targetYear().isBlank()) {
            interview.setTargetYear(request.targetYear());
        }

        ScheduledInterview saved = scheduledInterviewRepository.save(interview);
        return toScheduledInterviewRecord(saved);
    }

    /**
     * Admin assigns candidate to a scheduled interview and provisions candidate credentials.
     */
    @Transactional
    public CandidateAssignmentRecord assignCandidate(AssignCandidateRequest request) {
        log.info("Assigning candidate {} to interview {}", request.candidateEmail(), request.interviewId());

        ScheduledInterview interview = scheduledInterviewRepository.findById(request.interviewId())
                .orElseThrow(() -> new IllegalArgumentException("Scheduled interview not found: " + request.interviewId()));

        String email = request.candidateEmail().trim().toLowerCase();
        String name = request.candidateName() != null && !request.candidateName().isBlank()
                ? request.candidateName().trim()
                : email.split("@")[0];

        // Find or create candidate
        Candidate candidate = candidateRepository.findByEmail(email).orElseGet(() -> {
            Candidate newCand = new Candidate(name, "", List.of("General Software Engineering"));
            newCand.setEmail(email);
            return newCand;
        });

        candidate.setName(name);
        if (request.rollNo() != null && !request.rollNo().isBlank()) {
            candidate.setRollNo(request.rollNo().trim().toUpperCase());
        }
        if (request.studentYear() != null && !request.studentYear().isBlank()) {
            candidate.setStudentYear(request.studentYear());
        }
        if (request.department() != null && !request.department().isBlank()) {
            candidate.setDepartment(request.department());
        }
        if (request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            candidate.setPasswordHash(request.temporaryPassword());
        }
        candidate.setTemporaryPasswordIssued(request.isPermanent() != null && !request.isPermanent());
        candidate.setMustChangePassword(request.isPermanent() != null && !request.isPermanent());
        candidateRepository.save(candidate);

        // Check for existing assignment
        InterviewAssignment assignment = assignmentRepository
                .findByScheduledInterviewIdAndCandidateEmail(interview.getId(), email)
                .orElseGet(() -> {
                    InterviewAssignment newAssign = new InterviewAssignment(interview, candidate, email);
                    return assignmentRepository.save(newAssign);
                });

        return toCandidateAssignmentRecord(assignment);
    }

    @Transactional
    public CandidateAssignmentRecord assignCandidateByRollNo(UUID interviewId, String rollNo) {
        Candidate candidate = candidateRepository.findByRollNo(rollNo.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No student found with roll number: " + rollNo));

        AssignCandidateRequest request = new AssignCandidateRequest(
                interviewId,
                candidate.getName(),
                candidate.getEmail(),
                candidate.getRollNo(),
                candidate.getPasswordHash(),
                candidate.getStudentYear(),
                candidate.getDepartment(),
                true
        );
        return assignCandidate(request);
    }

    /**
     * Admin creates permanent student login credentials.
     */
    @Transactional
    public StudentResponse createStudentPermanently(CreateStudentRequest request) {
        String email = request.email().trim().toLowerCase();
        String name = request.name() != null && !request.name().isBlank() ? request.name().trim() : email.split("@")[0];
        String rollNo = request.rollNo() != null && !request.rollNo().isBlank() ? request.rollNo().trim().toUpperCase() : null;
        String year = request.studentYear() != null && !request.studentYear().isBlank() ? request.studentYear().trim() : "3rd Year";
        String dept = request.department() != null && !request.department().isBlank() ? request.department().trim() : "Computer Science & Engineering";
        String password = request.password() != null && !request.password().isBlank() ? request.password() : "Student@" + (int)(Math.random() * 9000 + 1000);

        Candidate candidate = candidateRepository.findByEmail(email).orElseGet(() -> {
            Candidate newCand = new Candidate(name, "", List.of("General Software Engineering"));
            newCand.setEmail(email);
            return newCand;
        });

        candidate.setName(name);
        candidate.setRollNo(rollNo);
        candidate.setStudentYear(year);
        candidate.setDepartment(dept);
        candidate.setPasswordHash(password);
        candidate.setTemporaryPasswordIssued(false);
        candidate.setMustChangePassword(false);

        Candidate saved = candidateRepository.save(candidate);
        log.info("Permanent student credentials created: {} ({}, {}, roll: {})", email, name, year, rollNo);

        emailService.sendStudentCredentials(email, name, rollNo, password, dept, year);

        return new StudentResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRollNo(),
                saved.getStudentYear(),
                saved.getDepartment(),
                password,
                saved.getCreatedAt()
        );
    }

    /**
     * Admin lists all registered students.
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> listAllStudents() {
        return candidateRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getEmail() != null)
                .map(c -> new StudentResponse(
                        c.getId(),
                        c.getName(),
                        c.getEmail(),
                        c.getRollNo(),
                        c.getStudentYear() != null ? c.getStudentYear() : "3rd Year",
                        c.getDepartment() != null ? c.getDepartment() : "Computer Science & Engineering",
                        null,
                        c.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Admin assigns an interview drive in bulk to all students of a specific year.
     */
    @Transactional
    public BulkAssignResponse assignDriveToYear(AssignYearRequest request) {
        ScheduledInterview interview = scheduledInterviewRepository.findById(request.interviewId())
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + request.interviewId()));

        String year = request.targetYear() != null ? request.targetYear().trim() : "ALL";
        List<Candidate> targetStudents;

        if (year.equalsIgnoreCase("ALL")) {
            targetStudents = candidateRepository.findAll();
        } else {
            targetStudents = candidateRepository.findByStudentYear(year);
        }

        int count = 0;
        for (Candidate student : targetStudents) {
            if (student.getEmail() == null || student.getEmail().isBlank()) continue;
            String email = student.getEmail().trim().toLowerCase();
            Optional<InterviewAssignment> existing = assignmentRepository.findByScheduledInterviewIdAndCandidateEmail(interview.getId(), email);
            if (existing.isEmpty()) {
                InterviewAssignment newAssign = new InterviewAssignment(interview, student, email);
                assignmentRepository.save(newAssign);
                count++;
            }
        }

        log.info("Bulk assigned interview '{}' to {} students of batch/year '{}'", interview.getTitle(), count, year);
        return new BulkAssignResponse(count, String.format("Successfully assigned '%s' to %d student(s) from %s.", interview.getTitle(), count, year));
    }

    /**
     * Student or Admin resets candidate password.
     */
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return new ResetPasswordResponse(false, "Email address is required.");
        }
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            return new ResetPasswordResponse(false, "Password must be at least 6 characters long.");
        }

        String email = request.email().trim().toLowerCase();
        Optional<Candidate> candOpt = candidateRepository.findByEmail(email);

        if (candOpt.isEmpty()) {
            Candidate newCand = new Candidate(email.split("@")[0], "", List.of("General"));
            newCand.setEmail(email);
            newCand.setPasswordHash(request.newPassword());
            newCand.setMustChangePassword(false);
            candidateRepository.save(newCand);
            return new ResetPasswordResponse(true, "Password updated successfully for " + email);
        }

        Candidate candidate = candOpt.get();
        candidate.setPasswordHash(request.newPassword());
        candidate.setMustChangePassword(false);
        candidateRepository.save(candidate);
        log.info("Password successfully updated for candidate: {}", email);

        return new ResetPasswordResponse(true, "Password has been successfully changed for " + email);
    }

    /**
     * Returns all scheduled interviews with attendance metrics.
     */
    @Transactional(readOnly = true)
    public List<ScheduledInterviewRecord> listAllScheduledInterviews() {
        List<ScheduledInterview> interviews = scheduledInterviewRepository.findAllOrderByCreatedAtDesc();
        List<ScheduledInterviewRecord> records = new ArrayList<>();

        for (ScheduledInterview interview : interviews) {
            records.add(toScheduledInterviewRecord(interview));
        }

        return records;
    }

    /**
     * Admin views attendance, candidate scores, and red-highlighted malpractice flags.
     */
    @Transactional(readOnly = true)
    public List<AttendanceMarksRecord> getInterviewAttendance(UUID interviewId) {
        log.info("Fetching attendance & marks for interview: {}", interviewId);
        List<InterviewAssignment> assignments = assignmentRepository.findByScheduledInterviewIdWithCandidateAndSession(interviewId);
        List<AttendanceMarksRecord> records = new ArrayList<>();

        for (InterviewAssignment a : assignments) {
            InterviewSession s = a.getSession();
            Candidate c = a.getCandidate();

            Double overall = null;
            Double tech = null;
            Double comm = null;
            String verdict = "Pending Evaluation";
            Boolean isMalpractice = false;
            Integer warningsCount = 0;
            String malpracticeReason = null;
            Instant startedAt = null;
            Instant endedAt = null;
            UUID sessionId = null;
            List<String> violationDetails = new ArrayList<>();

            if (s != null) {
                sessionId = s.getId();
                overall = s.getOverallScore();
                tech = s.getTechnicalScore();
                comm = s.getCommunicationScore();
                verdict = s.getVerdict() != null ? s.getVerdict() : "Completed";
                isMalpractice = s.getIsMalpractice() || s.getStatus() == SessionStatus.MALPRACTICE;
                warningsCount = s.getProctorWarningsCount();
                malpracticeReason = s.getMalpracticeReason();
                startedAt = s.getStartedAt();
                endedAt = s.getEndedAt();

                List<ProctorViolationLog> logs = violationLogRepository.findBySessionIdOrderByLoggedAtAsc(s.getId());
                for (ProctorViolationLog l : logs) {
                    violationDetails.add(String.format("Warning #%d [%s]: %s", l.getWarningNumber(), l.getViolationType(), l.getDetails()));
                }
            }

            String candidateRoll = c != null ? c.getRollNo() : null;

            records.add(new AttendanceMarksRecord(
                    a.getId(),
                    sessionId,
                    c.getId(),
                    c.getName(),
                    a.getCandidateEmail(),
                    candidateRoll,
                    a.getAttemptStatus().name(),
                    a.getAssignedAt(),
                    startedAt,
                    endedAt,
                    overall,
                    tech,
                    comm,
                    verdict,
                    isMalpractice,
                    warningsCount,
                    malpracticeReason,
                    violationDetails
            ));
        }

        return records;
    }

    /**
     * Candidate views all interviews assigned to their email address, including drives targeted for their batch/year.
     */
    @Transactional
    public List<CandidateAssignmentRecord> getCandidateAssignedInterviews(String candidateEmail) {
        if (candidateEmail == null || candidateEmail.isBlank()) {
            return List.of();
        }
        String email = candidateEmail.trim().toLowerCase();
        Optional<Candidate> candOpt = candidateRepository.findByEmail(email);

        if (candOpt.isPresent()) {
            Candidate candidate = candOpt.get();
            String studentYear = candidate.getStudentYear();
            List<ScheduledInterview> activeDrives = scheduledInterviewRepository.findAllOrderByCreatedAtDesc();
            for (ScheduledInterview drive : activeDrives) {
                if ("ACTIVE".equalsIgnoreCase(drive.getStatus())) {
                    String target = drive.getTargetYear() != null ? drive.getTargetYear() : "ALL";
                    if (target.equalsIgnoreCase("ALL") || (studentYear != null && target.equalsIgnoreCase(studentYear))) {
                        Optional<InterviewAssignment> existing = assignmentRepository.findByScheduledInterviewIdAndCandidateEmail(drive.getId(), email);
                        if (existing.isEmpty()) {
                            InterviewAssignment newAssign = new InterviewAssignment(drive, candidate, email);
                            assignmentRepository.save(newAssign);
                        }
                    }
                }
            }
        }

        List<InterviewAssignment> assignments = assignmentRepository.findByCandidateEmailWithDetails(email);
        return assignments.stream().map(this::toCandidateAssignmentRecord).toList();
    }

    /**
     * Verifies single attempt constraint & supports network resilience reconnection.
     */
    @Transactional(readOnly = true)
    public VerifyAttemptResponse verifyAttemptEligibility(UUID interviewId, String candidateEmail, String sessionToken) {
        log.info("Verifying attempt eligibility for interview: {}, email: {}", interviewId, candidateEmail);

        String email = candidateEmail.trim().toLowerCase();
        Optional<InterviewAssignment> assignOpt = assignmentRepository.findByScheduledInterviewIdAndCandidateEmail(interviewId, email);

        if (assignOpt.isEmpty()) {
            return new VerifyAttemptResponse(
                    false,
                    "You are not assigned to this interview drive. Please contact your administrator.",
                    null, interviewId, null, "NOT_ASSIGNED", 0, 0, null, null, false
            );
        }

        InterviewAssignment assignment = assignOpt.get();
        ScheduledInterview interview = assignment.getScheduledInterview();
        AssignmentStatus status = assignment.getAttemptStatus();

        if (status == AssignmentStatus.COMPLETED) {
            return new VerifyAttemptResponse(
                    false,
                    "You have already attended and completed this interview. Multiple attempts are strictly prohibited.",
                    assignment.getId(), interviewId, interview.getTitle(), status.name(),
                    interview.getDurationMinutes(), interview.getTotalQuestions(),
                    assignment.getSession() != null ? assignment.getSession().getId() : null,
                    null, false
            );
        }

        if (status == AssignmentStatus.DISQUALIFIED) {
            return new VerifyAttemptResponse(
                    false,
                    "This interview session was terminated due to a proctoring violation. Retakes are not permitted.",
                    assignment.getId(), interviewId, interview.getTitle(), status.name(),
                    interview.getDurationMinutes(), interview.getTotalQuestions(),
                    assignment.getSession() != null ? assignment.getSession().getId() : null,
                    null, false
            );
        }

        // Check if an ongoing session exists (network resilience / page refresh)
        InterviewSession session = assignment.getSession();
        if (status == AssignmentStatus.IN_PROGRESS && session != null) {
            return new VerifyAttemptResponse(
                    true,
                    "Active session detected. Resuming ongoing interview...",
                    assignment.getId(), interviewId, interview.getTitle(), status.name(),
                    interview.getDurationMinutes(), interview.getTotalQuestions(),
                    session.getId(), session.getSessionToken(), true
            );
        }

        // Permitted fresh attempt
        return new VerifyAttemptResponse(
                true,
                "Ready to start interview.",
                assignment.getId(), interviewId, interview.getTitle(), status.name(),
                interview.getDurationMinutes(), interview.getTotalQuestions(),
                null, null, false
        );
    }

    private ScheduledInterviewRecord toScheduledInterviewRecord(ScheduledInterview interview) {
        long totalAssigned = assignmentRepository.countByScheduledInterviewId(interview.getId());
        long completed = assignmentRepository.countByScheduledInterviewIdAndAttemptStatus(interview.getId(), AssignmentStatus.COMPLETED);
        long disqualified = assignmentRepository.countByScheduledInterviewIdAndAttemptStatus(interview.getId(), AssignmentStatus.DISQUALIFIED);

        List<InterviewAssignment> list = assignmentRepository.findByScheduledInterviewIdWithCandidateAndSession(interview.getId());
        double totalScore = 0;
        int scoreCount = 0;
        for (InterviewAssignment a : list) {
            if (a.getSession() != null && a.getSession().getOverallScore() != null) {
                totalScore += a.getSession().getOverallScore();
                scoreCount++;
            }
        }
        Double avg = scoreCount > 0 ? Math.round((totalScore / scoreCount) * 10.0) / 10.0 : null;

        return new ScheduledInterviewRecord(
                interview.getId(),
                interview.getTitle(),
                interview.getTargetRole(),
                interview.getDescription(),
                interview.getTotalQuestions(),
                interview.getDurationMinutes(),
                interview.getScheduledStart(),
                interview.getScheduledEnd(),
                interview.getStatus(),
                interview.getTargetYear() != null ? interview.getTargetYear() : "ALL",
                totalAssigned,
                completed,
                disqualified,
                avg,
                interview.getCreatedAt()
        );
    }

    private CandidateAssignmentRecord toCandidateAssignmentRecord(InterviewAssignment assignment) {
        ScheduledInterview i = assignment.getScheduledInterview();
        InterviewSession s = assignment.getSession();

        return new CandidateAssignmentRecord(
                assignment.getId(),
                i.getId(),
                i.getTitle(),
                i.getTargetRole(),
                i.getDescription(),
                i.getDurationMinutes(),
                i.getTotalQuestions(),
                assignment.getAttemptStatus().name(),
                i.getScheduledStart(),
                i.getScheduledEnd(),
                assignment.getAttemptedAt(),
                s != null ? s.getOverallScore() : null,
                s != null ? s.getTechnicalScore() : null,
                s != null ? s.getCommunicationScore() : null,
                s != null ? s.getVerdict() : null,
                i.getTargetYear() != null ? i.getTargetYear() : "ALL",
                s != null ? s.getId() : null,
                s != null ? s.getSessionToken() : null
        );
    }
}
