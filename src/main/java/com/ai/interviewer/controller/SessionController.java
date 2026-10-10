package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.service.InterviewSessionService;
import com.ai.interviewer.service.LiveKitTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final InterviewSessionService sessionService;
    private final LiveKitTokenService liveKitTokenService;

    public SessionController(InterviewSessionService sessionService, LiveKitTokenService liveKitTokenService) {
        this.sessionService = sessionService;
        this.liveKitTokenService = liveKitTokenService;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(@RequestBody StartSessionRequest request) {
        log.info("REST: Starting session for candidateId: {}", request.candidateId());
        SessionResponse response = sessionService.startSession(request.candidateId(), request.totalQuestions());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<TurnResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @RequestBody SubmitAnswerRequest request) {
        log.info("REST: Submitting answer for session: {}, question #{}", sessionId, request.questionNumber());
        TurnResponse response = sessionService.submitAnswer(
                sessionId,
                request.questionNumber(),
                request.candidateTranscript()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<FinalScorecardResponse> completeSession(@PathVariable UUID sessionId) {
        log.info("REST: Completing session: {}", sessionId);
        FinalScorecardResponse response = sessionService.completeSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionRecord> getSession(@PathVariable UUID sessionId) {
        log.info("REST: Fetching session: {}", sessionId);
        InterviewSessionRecord record = sessionService.getSession(sessionId);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{sessionId}/token")
    public ResponseEntity<LiveKitTokenResponse> getLiveKitToken(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "Candidate") String name) {
        log.info("REST: Fetching LiveKit token for session: {}", sessionId);
        LiveKitTokenResponse token = liveKitTokenService.generateToken(sessionId, UUID.randomUUID().toString(), name);
        return ResponseEntity.ok(token);
    }
}
