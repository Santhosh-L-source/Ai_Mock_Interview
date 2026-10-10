package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoiceOrchestratorServiceTest {

    private InterviewSessionService sessionService;
    private SpeechToTextService speechToTextService;
    private TextToSpeechService textToSpeechService;
    private LiveKitRoomService liveKitRoomService;
    private VoiceOrchestratorService voiceOrchestratorService;

    @BeforeEach
    void setUp() {
        sessionService = Mockito.mock(InterviewSessionService.class);
        speechToTextService = Mockito.mock(SpeechToTextService.class);
        textToSpeechService = Mockito.mock(TextToSpeechService.class);
        liveKitRoomService = Mockito.mock(LiveKitRoomService.class);

        voiceOrchestratorService = new VoiceOrchestratorService(
                sessionService,
                speechToTextService,
                textToSpeechService,
                liveKitRoomService
        );
    }

    @Test
    @DisplayName("Should handle manual 'Next Button' click and return next question with TTS audio")
    void testHandleNextButtonClick() {
        UUID sessionId = UUID.randomUUID();

        TurnEvaluation mockEval = new TurnEvaluation(
                8.5, 9.0,
                List.of("Identified HashMap", "Correct O(1) lookups"),
                List.of(),
                "Solid explanation",
                "Mention edge cases",
                "Excellent technical grasp",
                "Add boundary check"
        );

        GeneratedQuestionResponse nextQuestion = new GeneratedQuestionResponse(
                2,
                QuestionCategory.DSA_BASIC,
                "Array Manipulation",
                "How would you reverse an array in-place without extra space?",
                "Assess memory-efficient algorithms",
                "Two pointers approach with O(1) auxiliary space",
                120
        );

        TurnResponse mockTurnResponse = new TurnResponse(
                UUID.randomUUID(),
                1,
                "Explain how HashMaps work in Java",
                "HashMaps use hashing and buckets with O(1) average lookup.",
                mockEval,
                nextQuestion,
                false
        );

        when(sessionService.submitAnswer(eq(sessionId), eq(1), anyString())).thenReturn(mockTurnResponse);
        when(textToSpeechService.synthesizeSpeech(anyString())).thenReturn(new TtsResponse("audio/wav", "bWFnaWNCYXNlNjQ=", 3.5, "How would you reverse an array in-place?"));

        VoiceEventRequest request = new VoiceEventRequest(
                "NEXT_BUTTON_CLICKED",
                1,
                "HashMaps use hashing and buckets with O(1) average lookup.",
                false,
                45
        );

        VoiceEventResponse response = voiceOrchestratorService.handleVoiceEvent(sessionId, request);

        assertNotNull(response);
        assertEquals("NEXT_QUESTION_READY", response.eventType());
        assertEquals(sessionId, response.sessionId());
        assertEquals(2, response.currentQuestionNumber());
        assertFalse(response.isSessionCompleted());
        assertNotNull(response.nextQuestion());
        assertEquals(120, response.nextQuestion().timeLimitSeconds());
        assertEquals("bWFnaWNCYXNlNjQ=", response.ttsAudioBase64());

        verify(sessionService).submitAnswer(eq(sessionId), eq(1), eq("HashMaps use hashing and buckets with O(1) average lookup."));
        verify(liveKitRoomService).broadcastEvaluation(eq(sessionId), eq(1), eq(mockEval));
        verify(liveKitRoomService).broadcastQuestionStart(eq(sessionId), eq(nextQuestion));
    }

    @Test
    @DisplayName("Should handle automatic 'Timer Expiry' timeout gracefully")
    void testHandleTimerExpired() {
        UUID sessionId = UUID.randomUUID();

        TurnEvaluation timeoutEval = new TurnEvaluation(
                3.0, 3.0,
                List.of(),
                List.of("Candidate ran out of time"),
                "Candidate attempted prompt but did not finish.",
                "Structure answers faster.",
                "Time expired before full answer was provided.",
                "Start with the core idea upfront."
        );

        GeneratedQuestionResponse nextQuestion = new GeneratedQuestionResponse(
                3,
                QuestionCategory.BEHAVIORAL,
                "Project Collaboration",
                "Tell me about a team project and your role.",
                "Behavioral evaluation",
                "STAR format",
                90
        );

        TurnResponse mockTurnResponse = new TurnResponse(
                UUID.randomUUID(),
                2,
                "Question 2 text",
                "Partial answer before time expired...",
                timeoutEval,
                nextQuestion,
                false
        );

        when(sessionService.submitAnswer(eq(sessionId), eq(2), anyString())).thenReturn(mockTurnResponse);
        when(textToSpeechService.synthesizeSpeech(anyString())).thenReturn(new TtsResponse("audio/wav", "bWFnaWNCYXNlNjQ=", 2.5, "Tell me about a team project."));

        VoiceEventRequest request = new VoiceEventRequest(
                "TIMER_EXPIRED",
                2,
                "Partial answer before time expired...",
                true,
                120
        );

        VoiceEventResponse response = voiceOrchestratorService.handleVoiceEvent(sessionId, request);

        assertNotNull(response);
        assertEquals("NEXT_QUESTION_READY", response.eventType());
        assertEquals(3, response.currentQuestionNumber());
        assertFalse(response.isSessionCompleted());
        assertTrue(response.statusMessage().contains("Time expired"));
    }

    @Test
    @DisplayName("Should complete interview when all questions are answered")
    void testHandleSessionCompletion() {
        UUID sessionId = UUID.randomUUID();

        TurnResponse mockTurnResponse = new TurnResponse(
                UUID.randomUUID(),
                5,
                "Last question text",
                "Final answer...",
                null,
                null,
                true
        );

        FinalScorecardResponse scorecard = new FinalScorecardResponse(
                sessionId,
                "Candidate Name",
                8.5,
                8.7,
                8.2,
                "STRONG HIRE",
                "Outstanding performance",
                List.of()
        );

        when(sessionService.submitAnswer(eq(sessionId), eq(5), anyString())).thenReturn(mockTurnResponse);
        when(sessionService.completeSession(sessionId)).thenReturn(scorecard);
        when(textToSpeechService.synthesizeSpeech(anyString())).thenReturn(new TtsResponse("audio/wav", "bWFnaWNCYXNlNjQ=", 4.0, "Completed"));

        VoiceEventRequest request = new VoiceEventRequest("NEXT_BUTTON_CLICKED", 5, "Final answer...");
        VoiceEventResponse response = voiceOrchestratorService.handleVoiceEvent(sessionId, request);

        assertNotNull(response);
        assertEquals("INTERVIEW_COMPLETED", response.eventType());
        assertTrue(response.isSessionCompleted());
        assertNull(response.nextQuestion());
        verify(liveKitRoomService).broadcastInterviewCompleted(eq(sessionId), eq(8.5), eq("STRONG HIRE"));
    }
}
