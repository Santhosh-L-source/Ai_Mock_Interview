package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.CandidateAssignmentRecord;
import com.ai.interviewer.domain.dto.VerifyAttemptResponse;
import com.ai.interviewer.domain.entity.Candidate;
import com.ai.interviewer.repository.CandidateRepository;
import com.ai.interviewer.service.AdminSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/schedule")
@CrossOrigin(origins = "*")
public class CandidateScheduleController {

    private static final Logger log = LoggerFactory.getLogger(CandidateScheduleController.class);

    private final AdminSchedulingService schedulingService;
    private final CandidateRepository candidateRepository;

    public CandidateScheduleController(AdminSchedulingService schedulingService, CandidateRepository candidateRepository) {
        this.schedulingService = schedulingService;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Lists all interviews scheduled for the logged-in candidate.
     */
    @GetMapping("/my-interviews")
    public ResponseEntity<List<CandidateAssignmentRecord>> getMyInterviews(@RequestParam String email) {
        log.info("REST: Fetching assigned interviews for candidate: {}", email);
        List<CandidateAssignmentRecord> list = schedulingService.getCandidateAssignedInterviews(email);
        return ResponseEntity.ok(list);
    }

    /**
     * Pre-flight check: ensures single attempt constraint and supports network resumption.
     */
    @PostMapping("/verify-attempt")
    public ResponseEntity<VerifyAttemptResponse> verifyAttempt(@RequestBody Map<String, String> request) {
        String interviewIdStr = request.get("interviewId");
        String email = request.get("email");
        String sessionToken = request.get("sessionToken");

        if (interviewIdStr == null || email == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID interviewId = UUID.fromString(interviewIdStr);
        VerifyAttemptResponse response = schedulingService.verifyAttemptEligibility(interviewId, email, sessionToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks candidate password as updated.
     */
    @PostMapping("/password-status")
    public ResponseEntity<Map<String, Object>> updatePasswordStatus(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email != null) {
            candidateRepository.findByEmail(email.trim().toLowerCase()).ifPresent(c -> {
                c.setMustChangePassword(false);
                candidateRepository.save(c);
            });
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Student resets their password using their email/username.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<com.ai.interviewer.domain.dto.ResetPasswordResponse> resetPassword(@RequestBody com.ai.interviewer.domain.dto.ResetPasswordRequest request) {
        log.info("REST: Candidate resetting password for {}", request.email());
        com.ai.interviewer.domain.dto.ResetPasswordResponse response = schedulingService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
