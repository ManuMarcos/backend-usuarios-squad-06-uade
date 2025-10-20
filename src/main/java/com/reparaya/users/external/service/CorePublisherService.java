package com.reparaya.users.external.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorePublisherService {

    // TODO: store api key in env

    private final String SUBSCRIPTION_ID = "ce687bc8-7b6b-4a83-a34c-94c31114476e";
    private final String CORE_ACK_URL = "";
    private final String CORE_EVENT_PUBLISH_URL = "https://nonprodapi.uade-corehub.com/publish";

    public void sendAckToCore(final String messageId) {
        RestTemplate rt = new RestTemplate();
        String url = "https://nonprodapi.uade-corehub.com/messages/" + messageId + "/ack";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", "ch_9aa39636744843d880a69e45cd08f1ab");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("msgId", messageId, "subscriptionId", SUBSCRIPTION_ID), headers);
        log.info("Sending ACK for messageId {}", messageId);

        // TODO: uncomment
        //String response = rt.postForObject(url, entity, String.class);
        //log.info("Received ACK response {} from core for messageId {}", response, messageId);
    }

    public void sendUserCreatedToCore(RegisterResponse registerResponse) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", "ch_9aa39636744843d880a69e45cd08f1ab");

        Map<String, Object> userData = Map.of(
                "message", registerResponse.getMessage(),
                "email", registerResponse.getEmail(),
                "role", registerResponse.getRole()
        );

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "source", "users",
                "destination", Map.of(
                        "channel", "users.user.user_created",
                        "eventName", "user_created"
                ),
                "payload", userData
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user created event with messageId {}", messageId);

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user created response from core {} for messageId {}", response, messageId);
    }

    public void sendUserDeactivatedToCore(final String messageId) {
        // implement
    }

    public void sendUserUpdatedToCore(final String messageId) {
        // implement
    }
}
