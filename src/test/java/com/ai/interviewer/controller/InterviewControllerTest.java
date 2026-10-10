package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.domain.entity.SessionStatus;
import com.ai.interviewer.service.InterviewSessionService;
import com.ai.interviewer.service.LiveKitRoomService;
import com.ai.interviewer.service.TextToSpeechService;
import com.ai.interviewer.service.VoiceOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InterviewControllerTest {

    private InterviewSessionService sessionService;
    private VoiceOrchestratorService voiceOrchestratorService;
    private TextToSpeechService textToSpeechService;
    private LiveKitRoomService liveKitRoomService;
    private InterviewController interviewController;

    @BeforeEach
    void setUp() {
        sessionService = Mockito.mock(InterviewSessionService.class);
        voiceOrchestratorService = Mockito.mock(VoiceOrchestratorService.class);
        textToSpeechService = Mockito.mock(TextToSpeechService.class);
        liveKitRoomService = Mockito.mock(LiveKitRoomService.class);

        interviewController = new InterviewController(
                sessionService,
                voiceOrchestratorService,
                textToSpeechService,
                liveKitRoomService
        );
    }

    @Test
    @DisplayName("Should start interview and broadcast Question 1")
    void testStartInterview() {
        UUID candidateId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        GeneratedQuestionResponse q1 = new GeneratedQuestionResponse(
                1, QuestionCategory.RESUME_SKILL, "Python Functions", "Explain def vs lambda in Python.", "Basic syntax", "Functions syntax", 120
        );

        SessionResponse mockResponse = new SessionResponse(
                sessionId, candidateId, "Test Student", SessionStatus.IN_PROGRESS, Instant.now(), null, q1, null
        );

        when(sessionService.startSession(eq(candidateId), eq(5), any(), any())).thenReturn(mockResponse);

        ResponseEntity<SessionResponse> res = interviewController.startInterview(new StartSessionRequest(candidateId, 5));

        assertNotNull(res.getBody());
        assertEquals(sessionId, res.getBody().sessionId());
        verify(liveKitRoomService).broadcastQuestionStart(eq(sessionId), eq(q1));
    }

    @Test
    @DisplayName("Should process next question turn and return evaluation + next question")
    void testNextQuestion() {
        UUID sessionId = UUID.randomUUID();

        TurnEvaluation eval = new TurnEvaluation(
                8.0, 8.5, List.of("Covered syntax"), List.of(), "Good clarity", "Expand edge cases", "Solid", "Add examples"
        );

        GeneratedQuestionResponse nextQ = new GeneratedQuestionResponse(
                2, QuestionCategory.DSA_BASIC, "Two Pointers", "How do you check for a palindrome?", "DSA Basics", "O(N) check", 120
        );

        VoiceEventResponse voiceResponse = new VoiceEventResponse(
                "NEXT_QUESTION_READY", sessionId, 2, false, nextQ, eval, "bWFnaWM=", "Advancing to Q2"
        );

        when(voiceOrchestratorService.handleVoiceEvent(eq(sessionId), any(VoiceEventRequest.class))).thenReturn(voiceResponse);

        InterviewNextRequest request = new InterviewNextRequest(sessionId, 1, "In Python, def creates a standard function.", false, 40);
        ResponseEntity<InterviewNextResponse> res = interviewController.nextQuestion(request);

        assertNotNull(res.getBody());
        assertFalse(res.getBody().isSessionCompleted());
        assertEquals(1, res.getBody().answeredQuestionNumber());
        assertEquals(eval, res.getBody().evaluation());
        assertEquals(nextQ, res.getBody().nextQuestion());
        assertEquals("bWFnaWM=", res.getBody().ttsAudioBase64());
    }

    @Test
    @DisplayName("Should retrieve final scorecard summary")
    void testGetSummary() {
        UUID sessionId = UUID.randomUUID();
        FinalScorecardResponse scorecard = new FinalScorecardResponse(
                sessionId, "Alex", 8.8, 9.0, 8.5, "STRONG HIRE", "Great communication and coding skills.", List.of()
        );

        when(sessionService.completeSession(eq(sessionId))).thenReturn(scorecard);

        ResponseEntity<FinalScorecardResponse> res = interviewController.getInterviewSummary(sessionId);

        assertNotNull(res.getBody());
        assertEquals(8.8, res.getBody().overallScore());
        assertEquals("STRONG HIRE", res.getBody().overallVerdict());
    }

    @Test
    @DisplayName("Should record proctor violation and handle malpractice disqualification")
    void testRecordProctorViolation() {
        UUID sessionId = UUID.randomUUID();
        ProctorViolationRequest req = new ProctorViolationRequest(
                "MOBILE_DEVICE", "Mobile phone detected in frame", 3, true, Instant.now().toString()
        );
        ProctorViolationResponse mockResp = new ProctorViolationResponse(
                sessionId, 3, SessionStatus.MALPRACTICE, true, "Session terminated due to malpractice"
        );

        when(sessionService.recordProctorViolation(eq(sessionId), any(ProctorViolationRequest.class))).thenReturn(mockResp);

        ResponseEntity<ProctorViolationResponse> res = interviewController.recordProctorViolation(sessionId, req);

        assertNotNull(res.getBody());
        assertEquals(3, res.getBody().totalWarnings());
        assertTrue(res.getBody().disqualified());
        assertEquals(SessionStatus.MALPRACTICE, res.getBody().sessionStatus());
    }
}
