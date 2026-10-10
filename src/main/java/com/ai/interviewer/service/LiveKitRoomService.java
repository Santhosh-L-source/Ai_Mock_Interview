package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.GeneratedQuestionResponse;
import com.ai.interviewer.domain.dto.TurnEvaluation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveKitRoomService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitRoomService.class);

    @Value("${livekit.api-key:devkey}")
    private String apiKey;

    @Value("${livekit.api-secret:secret}")
    private String apiSecret;

    @Value("${livekit.url:ws://localhost:7880}")
    private String livekitUrl;

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> roomStateCache = new ConcurrentHashMap<>();

    public LiveKitRoomService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getRoomName(UUID sessionId) {
        return "interview-session-" + sessionId.toString();
    }

    public void broadcastQuestionStart(UUID sessionId, GeneratedQuestionResponse question) {
        String roomName = getRoomName(sessionId);
        log.info("Broadcasting 'QUESTION_START' for room '{}', Question #{}", roomName, question.questionNumber());

        Map<String, Object> payload = Map.of(
                "event", "QUESTION_START",
                "sessionId", sessionId.toString(),
                "questionNumber", question.questionNumber(),
                "topic", question.topic(),
                "category", question.category().name(),
                "questionText", question.questionText(),
                "timeLimitSeconds", question.timeLimitSeconds() != null ? question.timeLimitSeconds() : 120,
                "timestamp", Instant.now().toString()
        );

        roomStateCache.put(roomName, payload);
    }

    public void broadcastEvaluation(UUID sessionId, int questionNumber, TurnEvaluation evaluation) {
        String roomName = getRoomName(sessionId);
        log.info("Broadcasting 'EVALUATION_READY' for room '{}', Question #{}", roomName, questionNumber);

        Map<String, Object> payload = Map.of(
                "event", "EVALUATION_READY",
                "sessionId", sessionId.toString(),
                "questionNumber", questionNumber,
                "technicalScore", evaluation.technicalScore(),
                "communicationScore", evaluation.communicationScore(),
                "feedbackSummary", evaluation.feedbackSummary(),
                "timestamp", Instant.now().toString()
        );

        roomStateCache.put(roomName + "-last-eval", payload);
    }

    public void broadcastInterviewCompleted(UUID sessionId, double overallScore, String verdict) {
        String roomName = getRoomName(sessionId);
        log.info("Broadcasting 'INTERVIEW_COMPLETED' for room '{}' with verdict '{}'", roomName, verdict);

        Map<String, Object> payload = Map.of(
                "event", "INTERVIEW_COMPLETED",
                "sessionId", sessionId.toString(),
                "overallScore", overallScore,
                "verdict", verdict,
                "timestamp", Instant.now().toString()
        );

        roomStateCache.put(roomName + "-completed", payload);
    }

    public Map<String, Object> getLatestRoomState(UUID sessionId) {
        String roomName = getRoomName(sessionId);
        return roomStateCache.getOrDefault(roomName, Map.of(
                "roomName", roomName,
                "status", "ACTIVE",
                "livekitUrl", livekitUrl
        ));
    }
}
