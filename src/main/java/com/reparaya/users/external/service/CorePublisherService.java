package com.reparaya.users.external.service;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorePublisherService {
    // TODO: store api key in env
    public static final String API_KEY = "ch_9aa39636744843d880a69e45cd08f1ab";

    private final String CORE_EVENT_PUBLISH_URL = "https://nonprodapi.uade-corehub.com/publish";


    public void sendUserCreatedToCore(RegisterResponse registerResponse) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> userData = Map.of(
                "message", registerResponse.getMessage(),
                "user", registerResponse.getUser(),
                "zones", registerResponse.getZones() != null ? registerResponse.getZones() : Collections.emptyList(),
                "skills", registerResponse.getSkills() != null ? registerResponse.getSkills() : Collections.emptyList()
        );

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "destination", Map.of(
                        "topic", "user",
                        "eventName", "user_created"
                ),
                "payload", userData
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user created event to core with messageId: {}", messageId);

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user created response from core: {} for messageId: {}", response, messageId);

    }

    public void sendUserDeactivatedToCore(final String messageId) {
        // implement
    }

    public void sendUserUpdatedToCore(final String messageId) {
        // implement
    }

    public void sendUserRejectedToCore(final CoreMessage message, final String errorMessage) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> payload = Map.of(
                "message", "Usuario rechazado. Motivo: " + errorMessage);

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "destination", Map.of(
                        "topic", "user",
                        "eventName", "user_rejected"
                ),
                "payload", payload
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user rejected event to core with messageId: {} and reason: {} ", messageId, errorMessage);

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user rejected response from core: {} for messageId: {}", response, messageId);
    }
}