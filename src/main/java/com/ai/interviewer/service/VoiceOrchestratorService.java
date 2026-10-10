package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VoiceOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(VoiceOrchestratorService.class);

    private final InterviewSessionService sessionService;
    private final SpeechToTextService speechToTextService;
    private final TextToSpeechService textToSpeechService;
    private final LiveKitRoomService liveKitRoomService;

    public VoiceOrchestratorService(
            InterviewSessionService sessionService,
            SpeechToTextService speechToTextService,
            TextToSpeechService textToSpeechService,
            LiveKitRoomService liveKitRoomService) {
        this.sessionService = sessionService;
        this.speechToTextService = speechToTextService;
        this.textToSpeechService = textToSpeechService;
        this.liveKitRoomService = liveKitRoomService;
    }

    /**
     * Handles real-time candidate actions: manual 'Next' button clicks or automatic timer expirations.
     */
    public VoiceEventResponse handleVoiceEvent(UUID sessionId, VoiceEventRequest request) {
        String eventType = request.eventType() != null ? request.eventType() : "NEXT_BUTTON_CLICKED";
        boolean isTimeout = Boolean.TRUE.equals(request.isTimeout()) || "TIMER_EXPIRED".equalsIgnoreCase(eventType);

        log.info("Voice Orchestrator processing event '{}' for Session: {}, Question #{} (isTimeout: {})",
                eventType, sessionId, request.questionNumber(), isTimeout);

        String transcript = request.candidateTranscript();
        if (transcript == null || transcript.trim().isBlank()) {
            transcript = isTimeout ? "Time expired before candidate provided an answer." : "";
        }

        // 1. Submit answer and trigger evaluation + next question generation
        TurnResponse turnResponse = sessionService.submitAnswer(sessionId, request.questionNumber(), transcript);

        // 2. Broadcast evaluation results to LiveKit room
        if (turnResponse.evaluation() != null) {
            liveKitRoomService.broadcastEvaluation(sessionId, request.questionNumber(), turnResponse.evaluation());
        }

        // 3. Handle next question or session completion
        if (turnResponse.isCompleted() || turnResponse.nextQuestion() == null) {
            FinalScorecardResponse scorecard = sessionService.completeSession(sessionId);
            liveKitRoomService.broadcastInterviewCompleted(sessionId, scorecard.overallScore(), scorecard.overallVerdict());

            String completionText = "Thank you! You have completed all questions in the interview. Your final scorecard is now ready.";
            TtsResponse completionTts = textToSpeechService.synthesizeSpeech(completionText);

            return new VoiceEventResponse(
                    "INTERVIEW_COMPLETED",
                    sessionId,
                    request.questionNumber(),
                    true,
                    null,
                    turnResponse.evaluation(),
                    completionTts.audioBase64(),
                    "Interview successfully completed. Overall verdict: " + scorecard.overallVerdict()
            );
        } else {
            GeneratedQuestionResponse nextQ = turnResponse.nextQuestion();

            // 4. Synthesize speech for the next question
            TtsResponse tts = textToSpeechService.synthesizeSpeech(nextQ.questionText());

            // 5. Broadcast Question Start to LiveKit Room
            liveKitRoomService.broadcastQuestionStart(sessionId, nextQ);

            String statusMessage = isTimeout
                    ? "Time expired for Question #" + request.questionNumber() + ". Proceeding to Question #" + nextQ.questionNumber() + "."
                    : "Answer evaluated. Advancing to Question #" + nextQ.questionNumber() + ".";

            return new VoiceEventResponse(
                    "NEXT_QUESTION_READY",
                    sessionId,
                    nextQ.questionNumber(),
                    false,
                    nextQ,
                    turnResponse.evaluation(),
                    tts.audioBase64(),
                    statusMessage
            );
        }
    }

    /**
     * Transcribes incoming candidate audio stream or chunk, and optionally submits it.
     */
    public SttResponse processCandidateAudio(byte[] audioBytes, String mimeType) {
        return speechToTextService.transcribeAudioBytes(audioBytes, mimeType);
    }

    /**
     * Synthesizes audio for any given question text or greeting.
     */
    public TtsResponse synthesizeQuestionAudio(String text) {
        return textToSpeechService.synthesizeSpeech(text);
    }
}
