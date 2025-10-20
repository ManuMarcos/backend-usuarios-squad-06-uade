package com.reparaya.users.external.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorePublisherService {
    // TODO: store api key in env
    public static final String API_KEY = "ch_9aa39636744843d880a69e45cd08f1ab";

    // TODO: manejar subscription id por cada topico
    private final String PRESTADORES_ALTA_SUBSCRIPTION_ID = "ce687bc8-7b6b-4a83-a34c-94c31114476e";

    private final String CORE_ACK_URL = "https://nonprodapi.uade-corehub.com/messages/";
    private final String CORE_EVENT_PUBLISH_URL = "https://nonprodapi.uade-corehub.com/publish";

    public void sendAckToCore(final String messageId) {
        RestTemplate rt = new RestTemplate();
        String url = CORE_ACK_URL + messageId + "/ack";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("msgId", messageId, "subscriptionId", PRESTADORES_ALTA_SUBSCRIPTION_ID), headers);
        log.info("Sending ACK for messageId {}", messageId);

        // TODO: comentado por ahora. Ver que pasa con el subscription id.

        //String response = rt.postForObject(url, entity, String.class);
        //log.info("Received ACK response {} from core for messageId {}", response, messageId);
    }

    public void sendUserCreatedToCore(RegisterResponse registerResponse) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

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

    public void sendUserRejectedToCore(final CoreMessage message, final String errorMessage) {
        UUID messageId = UUID.randomUUID();

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", API_KEY);

        Map<String, Object> payload = Map.of(
                "message", "Usuario rechazado. Motivo: " + errorMessage,
                "email", message.getPayload().get("email")
        );

        Map<String, Object> body = Map.of(
                "messageId", messageId,
                "timestamp", OffsetDateTime.now().toString(),
                "source", "users",
                "destination", Map.of(
                        "channel", "users.user.user_rejected",
                        "eventName", "user_rejected"
                ),
                "payload", payload
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending user rejected event with messageId {}", messageId);

        String response = rt.postForObject(CORE_EVENT_PUBLISH_URL, entity, String.class);
        log.info("Received user rejected response from core {} for messageId {}", response, messageId);
    }
}