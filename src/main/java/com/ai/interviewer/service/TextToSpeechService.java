package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.TtsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class TextToSpeechService {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechService.class);

    private final WebClient webClient;

    @Value("${openai.tts.api-key:${langchain4j.open-ai.chat-model.api-key:}}")
    private String openAiKey;

    @Value("${openai.tts.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.tts.model:tts-1}")
    private String ttsModel;

    @Value("${openai.tts.voice:alloy}")
    private String ttsVoice;

    @Value("${elevenlabs.api-key:}")
    private String elevenLabsKey;

    @Value("${cartesia.api-key:}")
    private String cartesiaKey;

    public TextToSpeechService() {
        this.webClient = WebClient.builder().build();
    }

    public TtsResponse synthesizeSpeech(String text) {
        if (text == null || text.isBlank()) {
            return new TtsResponse("audio/wav", "", 0.0, "");
        }

        log.info("Synthesizing speech for text of length {}: '{}...'",
                text.length(), text.substring(0, Math.min(40, text.length())));

        byte[] audioBytes = synthesizeSpeechBytes(text);
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        double estimatedDuration = Math.max(1.5, text.split("\\s+").length * 0.35);

        return new TtsResponse(
                "audio/wav",
                base64Audio,
                Math.round(estimatedDuration * 10.0) / 10.0,
                text
        );
    }

    public byte[] synthesizeSpeechBytes(String text) {
        // 1. Try OpenAI TTS if configured with standard OpenAI endpoint
        if (openAiKey != null && !openAiKey.isBlank() && !openAiKey.startsWith("gsk_") && openAiBaseUrl.contains("openai.com")) {
            try {
                byte[] responseBytes = webClient.post()
                        .uri(openAiBaseUrl + "/audio/speech")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "model", ttsModel,
                                "input", text,
                                "voice", ttsVoice,
                                "response_format", "wav"
                        ))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

                if (responseBytes != null && responseBytes.length > 0) {
                    return responseBytes;
                }
            } catch (Exception e) {
                log.warn("External OpenAI TTS call failed, falling back to built-in audio synthesizer: {}", e.getMessage());
            }
        }

        // 2. Built-in zero-latency WAV synthesizer fallback
        return generateSyntheticWav(text);
    }

    /**
     * Generates a valid RIFF WAV audio stream with gentle frequency envelope for local development & testing.
     */
    private byte[] generateSyntheticWav(String text) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            int sampleRate = 16000;
            int numWords = Math.max(3, text.split("\\s+").length);
            int durationSeconds = Math.min(10, Math.max(2, (int) (numWords * 0.3)));
            int totalSamples = sampleRate * durationSeconds;
            int byteRate = sampleRate * 2; // 16-bit mono

            // RIFF Header
            writeString(dos, "RIFF");
            writeInt(dos, 36 + totalSamples * 2);
            writeString(dos, "WAVE");

            // fmt subchunk
            writeString(dos, "fmt ");
            writeInt(dos, 16);          // Subchunk1Size (16 for PCM)
            writeShort(dos, 1);         // AudioFormat (1 for PCM)
            writeShort(dos, 1);         // NumChannels (1 = Mono)
            writeInt(dos, sampleRate);  // SampleRate
            writeInt(dos, byteRate);    // ByteRate
            writeShort(dos, 2);         // BlockAlign
            writeShort(dos, 16);        // BitsPerSample

            // data subchunk
            writeString(dos, "data");
            writeInt(dos, totalSamples * 2);

            // Generate soft acoustic tone wave for speech playback simulation
            double freq = 220.0;
            for (int i = 0; i < totalSamples; i++) {
                double time = (double) i / sampleRate;
                double modulation = 0.5 + 0.5 * Math.sin(2 * Math.PI * 4 * time);
                short sample = (short) (Math.sin(2 * Math.PI * freq * time) * 1200 * modulation);
                dos.writeByte(sample & 0xFF);
                dos.writeByte((sample >> 8) & 0xFF);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate synthetic WAV: {}", e.getMessage());
            return new byte[0];
        }
    }

    private void writeString(DataOutputStream dos, String s) throws IOException {
        dos.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeInt(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
        dos.writeByte((v >> 16) & 0xFF);
        dos.writeByte((v >> 24) & 0xFF);
    }

    private void writeShort(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }
}
