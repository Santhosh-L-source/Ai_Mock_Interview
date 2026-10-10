package com.ai.interviewer.config;

import com.ai.interviewer.domain.entity.*;
import com.ai.interviewer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final CandidateRepository candidateRepository;
    private final ScheduledInterviewRepository scheduledInterviewRepository;
    private final InterviewAssignmentRepository assignmentRepository;

    public DemoDataInitializer(
            AdminUserRepository adminUserRepository,
            CandidateRepository candidateRepository,
            ScheduledInterviewRepository scheduledInterviewRepository,
            InterviewAssignmentRepository assignmentRepository) {
        this.adminUserRepository = adminUserRepository;
        this.candidateRepository = candidateRepository;
        this.scheduledInterviewRepository = scheduledInterviewRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking & seeding demo student, admin, and scheduled drive records...");

        try {
            // 1. Seed Demo Admin Account
            AdminUser admin = adminUserRepository.findByUsernameOrEmail("admin", "admin@interview.ai")
                    .orElseGet(() -> {
                        AdminUser newAdmin = new AdminUser(
                                "admin",
                                "admin@interview.ai",
                                "Admin@2026",
                                "Chief Technical Evaluator (Admin)",
                                "ADMIN"
                        );
                        return adminUserRepository.save(newAdmin);
                    });
            log.info("Demo Admin verified: {} ({}) / pwd: Admin@2026", admin.getUsername(), admin.getEmail());

            // 2. Seed Demo Student Account (Roll No: 24AM119)
            Candidate student = candidateRepository.findByRollNo("24AM119")
                    .or(() -> candidateRepository.findByEmail("alex.morgan@testcampus.edu"))
                    .orElseGet(() -> {
                        Candidate newStudent = new Candidate(
                                "Alex Morgan",
                                "alex.morgan@testcampus.edu",
                                null,
                                "2nd-year AI & Machine Learning student (Roll No: 24AM119). Proficient in Python, Java, React, PostgreSQL, and Git. Coursework: Data Structures & Algorithms, Object-Oriented Programming, and Machine Learning Fundamentals. Built a real-time event booking portal with hash-based indexing and concurrency handling.",
                                List.of("Python", "Java", "React", "PostgreSQL", "Data Structures", "Git")
                        );
                        newStudent.setRollNo("24AM119");
                        newStudent.setPasswordHash("Candidate@2026");
                        newStudent.setStudentYear("2nd Year");
                        newStudent.setDepartment("Artificial Intelligence & Machine Learning");
                        newStudent.setTemporaryPasswordIssued(false);
                        newStudent.setMustChangePassword(false);
                        return candidateRepository.save(newStudent);
                    });

            if (student.getRollNo() == null || student.getRollNo().isBlank()) {
                student.setRollNo("24AM119");
                candidateRepository.save(student);
            }
            log.info("Demo Student verified: {} [Roll: {}] ({}) / pwd: Candidate@2026",
                    student.getName(), student.getRollNo(), student.getEmail());

            // 3. Seed Demo Scheduled Interview Drive
            ScheduledInterview drive = scheduledInterviewRepository.findAllOrderByCreatedAtDesc().stream()
                    .filter(d -> d.getTitle().contains("Campus Placement Technical Drive 2026"))
                    .findFirst()
                    .orElseGet(() -> {
                        ScheduledInterview newDrive = new ScheduledInterview(
                                "Campus Placement Technical Drive 2026",
                                "Software Development Engineer (SDE Intern)",
                                "Comprehensive AI-driven technical screening evaluating programming fundamentals, basic data structures & algorithms (Arrays, Strings, HashMaps, Two Pointers), and problem-solving experience.",
                                5,
                                15,
                                Instant.now(),
                                Instant.now().plusSeconds(86400 * 30)
                        );
                        newDrive.setTargetYear("ALL");
                        newDrive.setStatus("ACTIVE");
                        return scheduledInterviewRepository.save(newDrive);
                    });

            // 4. Assign Demo Student to Demo Scheduled Drive
            assignmentRepository.findByScheduledInterviewIdAndCandidateEmail(drive.getId(), student.getEmail())
                    .orElseGet(() -> {
                        InterviewAssignment assignment = new InterviewAssignment(drive, student, student.getEmail());
                        assignment.setAttemptStatus(AssignmentStatus.NOT_STARTED);
                        return assignmentRepository.save(assignment);
                    });

            log.info("Pre-assigned demo drive '{}' to student {} ({})", drive.getTitle(), student.getName(), student.getEmail());
        } catch (Exception e) {
            log.warn("Could not complete automatic demo data seeding (tables may be pending migration): {}", e.getMessage());
        }
    }
}
