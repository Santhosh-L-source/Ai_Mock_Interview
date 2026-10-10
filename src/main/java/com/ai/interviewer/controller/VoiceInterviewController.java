package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.service.LiveKitRoomService;
import com.ai.interviewer.service.VoiceOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceInterviewController {

    private static final Logger log = LoggerFactory.getLogger(VoiceInterviewController.class);

    private final VoiceOrchestratorService voiceOrchestratorService;
    private final LiveKitRoomService liveKitRoomService;

    public VoiceInterviewController(
            VoiceOrchestratorService voiceOrchestratorService,
            LiveKitRoomService liveKitRoomService) {
        this.voiceOrchestratorService = voiceOrchestratorService;
        this.liveKitRoomService = liveKitRoomService;
    }

    /**
     * Handles candidate action events: Next button click or automatic timer expiry.
     */
    @PostMapping("/session/{sessionId}/event")
    public ResponseEntity<VoiceEventResponse> handleVoiceEvent(
            @PathVariable UUID sessionId,
            @RequestBody VoiceEventRequest request) {
        log.info("Received voice event '{}' for session {}", request.eventType(), sessionId);
        VoiceEventResponse response = voiceOrchestratorService.handleVoiceEvent(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts text into synthesized speech audio.
     */
    @PostMapping("/session/{sessionId}/tts")
    public ResponseEntity<TtsResponse> synthesizeSpeech(
            @PathVariable UUID sessionId,
            @RequestBody TtsRequest request) {
        log.info("Synthesizing TTS audio for session {}", sessionId);
        TtsResponse response = voiceOrchestratorService.synthesizeQuestionAudio(request.text());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns raw audio/wav stream for direct browser audio element playback.
     */
    @PostMapping(value = "/session/{sessionId}/tts/raw", produces = "audio/wav")
    public ResponseEntity<byte[]> synthesizeSpeechRaw(
            @PathVariable UUID sessionId,
            @RequestBody TtsRequest request) {
        log.info("Generating raw audio/wav for session {}", sessionId);
        TtsResponse response = voiceOrchestratorService.synthesizeQuestionAudio(request.text());
        byte[] audioBytes = Base64.getDecoder().decode(response.audioBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"question-audio.wav\"")
                .body(audioBytes);
    }

    /**
     * Transcribes uploaded audio recording from candidate.
     */
    @PostMapping(value = "/session/{sessionId}/stt", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<SttResponse> transcribeAudio(
            @PathVariable UUID sessionId,
            @RequestBody byte[] audioBytes,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, defaultValue = "audio/wav") String mimeType) {
        log.info("Transcribing audio payload of {} bytes for session {}", audioBytes.length, sessionId);
        SttResponse response = voiceOrchestratorService.processCandidateAudio(audioBytes, mimeType);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves current LiveKit room state and token connection info.
     */
    @GetMapping("/session/{sessionId}/room")
    public ResponseEntity<Map<String, Object>> getRoomState(@PathVariable UUID sessionId) {
        Map<String, Object> state = liveKitRoomService.getLatestRoomState(sessionId);
        return ResponseEntity.ok(state);
    }
}
