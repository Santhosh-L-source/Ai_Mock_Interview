package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.service.AdminSchedulingService;
import com.ai.interviewer.service.ExcelExportService;
import com.ai.interviewer.service.InterviewSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/schedule")
@CrossOrigin(origins = "*")
public class AdminScheduleController {

    private static final Logger log = LoggerFactory.getLogger(AdminScheduleController.class);

    private final AdminSchedulingService adminSchedulingService;
    private final InterviewSessionService interviewSessionService;
    private final ExcelExportService excelExportService;

    public AdminScheduleController(AdminSchedulingService adminSchedulingService,
                                   InterviewSessionService interviewSessionService,
                                   ExcelExportService excelExportService) {
        this.adminSchedulingService = adminSchedulingService;
        this.interviewSessionService = interviewSessionService;
        this.excelExportService = excelExportService;
    }

    /**
     * Admin creates a new scheduled interview.
     */
    @PostMapping("/interviews")
    public ResponseEntity<ScheduledInterviewRecord> createInterview(@RequestBody CreateScheduledInterviewRequest request) {
        log.info("REST: Admin creating scheduled interview: {}", request.title());
        ScheduledInterviewRecord record = adminSchedulingService.createScheduledInterview(request);
        return ResponseEntity.ok(record);
    }

    /**
     * Admin lists all scheduled interviews with attendance & score summaries.
     */
    @GetMapping("/interviews")
    public ResponseEntity<List<ScheduledInterviewRecord>> listInterviews() {
        log.info("REST: Admin listing scheduled interviews");
        List<ScheduledInterviewRecord> list = adminSchedulingService.listAllScheduledInterviews();
        return ResponseEntity.ok(list);
    }

    /**
     * Admin assigns a candidate and provisions initial login credentials.
     */
    @PostMapping("/assign")
    public ResponseEntity<CandidateAssignmentRecord> assignCandidate(@RequestBody AssignCandidateRequest request) {
        log.info("REST: Admin assigning candidate {} to interview {}", request.candidateEmail(), request.interviewId());
        CandidateAssignmentRecord record = adminSchedulingService.assignCandidate(request);
        return ResponseEntity.ok(record);
    }

    /**
     * Admin assigns an existing student to an interview by roll number only.
     */
    @PostMapping("/assign-by-roll")
    public ResponseEntity<?> assignByRollNo(@RequestBody java.util.Map<String, String> body) {
        String interviewId = body.get("interviewId");
        String rollNo = body.get("rollNo");
        log.info("REST: Admin assigning student by roll no {} to interview {}", rollNo, interviewId);
        try {
            CandidateAssignmentRecord record = adminSchedulingService.assignCandidateByRollNo(
                    UUID.fromString(interviewId), rollNo);
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin views attendance, candidate scores, and red-highlighted malpractice flags for a specific interview.
     */
    @GetMapping("/interviews/{interviewId}/attendance")
    public ResponseEntity<List<AttendanceMarksRecord>> getInterviewAttendance(@PathVariable UUID interviewId) {
        log.info("REST: Admin fetching attendance & marks for interview {}", interviewId);
        List<AttendanceMarksRecord> records = adminSchedulingService.getInterviewAttendance(interviewId);
        return ResponseEntity.ok(records);
    }

    /**
     * Admin creates permanent student login credentials.
     */
    @PostMapping("/students")
    public ResponseEntity<StudentResponse> createStudent(@RequestBody CreateStudentRequest request) {
        log.info("REST: Admin creating student credentials for {}", request.email());
        StudentResponse response = adminSchedulingService.createStudentPermanently(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin lists all registered students.
     */
    @GetMapping("/students")
    public ResponseEntity<List<StudentResponse>> listStudents() {
        log.info("REST: Admin listing all students");
        List<StudentResponse> list = adminSchedulingService.listAllStudents();
        return ResponseEntity.ok(list);
    }

    /**
     * Admin assigns interview drive to all students of a specific year.
     */
    @PostMapping("/assign-year")
    public ResponseEntity<BulkAssignResponse> assignYear(@RequestBody AssignYearRequest request) {
        log.info("REST: Admin assigning interview {} to target year {}", request.interviewId(), request.targetYear());
        BulkAssignResponse response = adminSchedulingService.assignDriveToYear(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin views turn-by-turn scorecard detail for a candidate session.
     */
    @GetMapping("/sessions/{sessionId}/scorecard")
    public ResponseEntity<FinalScorecardResponse> getSessionScorecard(@PathVariable UUID sessionId) {
        log.info("REST: Admin viewing detailed scorecard for session {}", sessionId);
        FinalScorecardResponse scorecard = interviewSessionService.completeSession(sessionId);
        return ResponseEntity.ok(scorecard);
    }

    /**
     * Admin exports interview results as an Excel (.xlsx) file.
     */
    @GetMapping("/interviews/{interviewId}/export")
    public ResponseEntity<byte[]> exportInterviewResults(@PathVariable UUID interviewId) {
        log.info("REST: Admin exporting Excel results for interview {}", interviewId);
        try {
            byte[] excelBytes = excelExportService.generateInterviewReport(interviewId);
            String filename = "Interview_Results_" + interviewId.toString().substring(0, 8) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelBytes.length)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export Excel report for interview {}: {}", interviewId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
