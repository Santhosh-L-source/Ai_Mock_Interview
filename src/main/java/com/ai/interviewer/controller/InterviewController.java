package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.service.InterviewSessionService;
import com.ai.interviewer.service.LiveKitRoomService;
import com.ai.interviewer.service.TextToSpeechService;
import com.ai.interviewer.service.VoiceOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "*")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewSessionService sessionService;
    private final VoiceOrchestratorService voiceOrchestratorService;
    private final TextToSpeechService textToSpeechService;
    private final LiveKitRoomService liveKitRoomService;

    public InterviewController(
            InterviewSessionService sessionService,
            VoiceOrchestratorService voiceOrchestratorService,
            TextToSpeechService textToSpeechService,
            LiveKitRoomService liveKitRoomService) {
        this.sessionService = sessionService;
        this.voiceOrchestratorService = voiceOrchestratorService;
        this.textToSpeechService = textToSpeechService;
        this.liveKitRoomService = liveKitRoomService;
    }

    /**
     * Starts the interview session, generates Question #1, and initializes LiveKit WebRTC credentials.
     */
    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startInterview(@RequestBody StartSessionRequest request) {
        log.info("InterviewController: Starting interview for candidate: {}", request.candidateId());
        SessionResponse response = sessionService.startSession(
                request.candidateId(),
                request.totalQuestions(),
                request.assignmentId(),
                request.sessionToken()
        );

        // Broadcast Question #1 to LiveKit room
        if (response.currentQuestion() != null) {
            liveKitRoomService.broadcastQuestionStart(response.sessionId(), response.currentQuestion());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Submits candidate answer (via Next button or Timer expiry), evaluates turn, and yields Question N+1 with audio.
     */
    @PostMapping("/next")
    public ResponseEntity<InterviewNextResponse> nextQuestion(@RequestBody InterviewNextRequest request) {
        log.info("InterviewController: Processing next turn for session: {}, question #{}",
                request.sessionId(), request.questionNumber());

        String eventType = Boolean.TRUE.equals(request.isTimeout()) ? "TIMER_EXPIRED" : "NEXT_BUTTON_CLICKED";
        VoiceEventRequest voiceRequest = new VoiceEventRequest(
                eventType,
                request.questionNumber(),
                request.candidateTranscript(),
                request.isTimeout(),
                request.timeTakenSeconds()
        );

        VoiceEventResponse voiceResponse = voiceOrchestratorService.handleVoiceEvent(request.sessionId(), voiceRequest);

        FinalScorecardResponse summary = null;
        if (voiceResponse.isSessionCompleted()) {
            summary = sessionService.completeSession(request.sessionId());
        }

        InterviewNextResponse nextResponse = new InterviewNextResponse(
                request.sessionId(),
                request.questionNumber(),
                voiceResponse.isSessionCompleted(),
                voiceResponse.evaluation(),
                voiceResponse.nextQuestion(),
                voiceResponse.ttsAudioBase64(),
                summary
        );

        return ResponseEntity.ok(nextResponse);
    }

    /**
     * Generates structured final scorecard with weighted technical/communication metrics & verdict.
     */
    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<FinalScorecardResponse> getInterviewSummary(@PathVariable UUID sessionId) {
        log.info("InterviewController: Generating final scorecard for session: {}", sessionId);
        FinalScorecardResponse scorecard = sessionService.completeSession(sessionId);
        return ResponseEntity.ok(scorecard);
    }

    /**
     * Retrieves full session entity state with all turns and evaluations.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<InterviewSessionRecord> getSessionRecord(@PathVariable UUID sessionId) {
        log.info("InterviewController: Fetching session record: {}", sessionId);
        InterviewSessionRecord sessionRecord = sessionService.getSession(sessionId);
        return ResponseEntity.ok(sessionRecord);
    }

    /**
     * Records a proctoring violation (tab switch, mobile detection, fullscreen exit).
     * If violation threshold (3 warnings) is reached, session is disqualified for malpractice.
     */
    @PostMapping("/session/{sessionId}/proctor-violation")
    public ResponseEntity<ProctorViolationResponse> recordProctorViolation(
            @PathVariable UUID sessionId,
            @RequestBody ProctorViolationRequest request) {
        log.warn("InterviewController: Recording proctor violation for session: {}, type: {}",
                sessionId, request.violationType());
        ProctorViolationResponse response = sessionService.recordProctorViolation(sessionId, request);
        return ResponseEntity.ok(response);
    }
}
