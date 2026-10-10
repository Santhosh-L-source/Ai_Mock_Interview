package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.SttResponse;
import com.ai.interviewer.domain.dto.TtsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AudioServicesTest {

    @Test
    @DisplayName("TextToSpeechService should synthesize valid WAV audio bytes and base64 string")
    void testSynthesizeSpeech() {
        TextToSpeechService ttsService = new TextToSpeechService();

        String questionText = "Could you walk me through your experience with Python list comprehensions and dictionaries?";
        TtsResponse response = ttsService.synthesizeSpeech(questionText);

        assertNotNull(response);
        assertEquals("audio/wav", response.audioFormat());
        assertNotNull(response.audioBase64());
        assertFalse(response.audioBase64().isBlank());
        assertTrue(response.durationSeconds() > 0);

        byte[] decoded = Base64.getDecoder().decode(response.audioBase64());
        assertTrue(decoded.length > 44, "Audio file must contain valid WAV headers");

        // Verify RIFF and WAVE header tags
        String header = new String(decoded, 0, 4);
        assertEquals("RIFF", header);
        String format = new String(decoded, 8, 4);
        assertEquals("WAVE", format);
    }

    @Test
    @DisplayName("SpeechToTextService should transcribe audio bytes and base64 input")
    void testTranscribeAudio() {
        SpeechToTextService sttService = new SpeechToTextService(new ObjectMapper());

        byte[] mockWav = new byte[100];
        SttResponse response = sttService.transcribeAudioBytes(mockWav, "audio/wav");

        assertNotNull(response);
        assertNotNull(response.transcript());
        assertTrue(response.confidence() > 0.0);
        assertTrue(response.isFinal());

        // Test base64 overload
        String b64 = Base64.getEncoder().encodeToString(mockWav);
        SttResponse b64Response = sttService.transcribeBase64Audio(b64, "audio/wav");
        assertNotNull(b64Response);
        assertNotNull(b64Response.transcript());
    }
}
