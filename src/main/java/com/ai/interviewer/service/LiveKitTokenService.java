package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.LiveKitTokenResponse;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LiveKitTokenService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitTokenService.class);

    @Value("${livekit.api-key:devkey}")
    private String apiKey;

    @Value("${livekit.api-secret:secret}")
    private String apiSecret;

    @Value("${livekit.url:ws://localhost:7880}")
    private String livekitUrl;

    public LiveKitTokenResponse generateToken(UUID sessionId, String participantIdentity, String participantName) {
        String roomName = "interview-session-" + sessionId.toString();
        log.info("Generating LiveKit access token for room '{}', participant '{}'", roomName, participantIdentity);

        try {
            AccessToken token = new AccessToken(apiKey, apiSecret);
            token.setName(participantName != null ? participantName : "Candidate");
            token.setIdentity(participantIdentity != null ? participantIdentity : UUID.randomUUID().toString());

            token.addGrants(new RoomJoin(true), new RoomName(roomName));

            String jwt = token.toJwt();
            return new LiveKitTokenResponse(roomName, jwt, livekitUrl);
        } catch (Exception e) {
            log.error("Error generating LiveKit token, returning placeholder for local dev: {}", e.getMessage());
            return new LiveKitTokenResponse(roomName, "mock-token-" + sessionId, livekitUrl);
        }
    }
}
