package com.reparaya.users.external.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.UpdateUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorePublisherService {
    // TODO: store api key in env

    @Value("api.core.key")
    private final String API_KEY;

    private final String CORE_EVENT_PUBLISH_URL = "https://nonprodapi.uade-corehub.com/publish";


    public void sendUserCreatedToCore(RegisterResponse registerResponse) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> userData = new HashMap<>();

        userData.put("message", registerResponse.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> userMap = mapper.convertValue(registerResponse.getUser(), Map.class);
        userData.putAll(userMap);

        // puede que vengan null las zonas y skills
        userData.put("zones", registerResponse.getZones());
        userData.put("skills", registerResponse.getSkills());



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
        log.info("Sending user created event to core with messageId: {} and email_ {}", messageId, registerResponse.getUser().getEmail());

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user created response from core: {} for messageId: {}", response, messageId);

    }

    public void sendUserDeactivatedToCore(final Long userId) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> payload = Map.of(
                "message", "Usuario dado de baja exitosamente",
                "userId", userId);

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "destination", Map.of(
                        "topic", "user",
                        "eventName", "user_deactivated"
                ),
                "payload", payload
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user deactivated event to core with messageId: {} and userId: {} ", messageId, userId);

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user deactivated response from core: {} for messageId: {}", response, messageId);
    }

    public void sendUserUpdatedToCore(final UpdateUserResponse updateResponse) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> userData = new HashMap<>();

        userData.put("message", "Usuario actualizado exitosamente");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> userMap = mapper.convertValue(updateResponse.getUser(), Map.class);
        userData.putAll(userMap);

        userData.put("zones", updateResponse.getZones());
        userData.put("skills", updateResponse.getSkills());

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "destination", Map.of(
                        "topic", "user",
                        "eventName", "user_updated"
                ),
                "payload", userData
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user updated event to core with messageId: {} and email_ {}", messageId, updateResponse.getUser().getEmail());

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user updated response from core: {} for messageId: {}", response, messageId);
    }

    public void sendUserRejectedToCore(final String email, final String errorMessage) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> payload = Map.of(
                "message", "Usuario rechazado. Motivo: " + errorMessage,
                "email", email);

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