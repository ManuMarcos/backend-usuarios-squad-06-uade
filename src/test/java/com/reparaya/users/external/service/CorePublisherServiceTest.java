package com.reparaya.users.external.service;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorePublisherServiceTest {

    private CorePublisherService newService() {
        CorePublisherService service = new CorePublisherService();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        return service;
    }

    @Test
    void sendUserCreatedToCore_PublishesExpectedPayload() {
        RegisterResponse response = RegisterResponse.builder()
                .message("Usuario creado")
                .user(UserDto.builder()
                        .email("user@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .address(List.of(AddressInfo.builder()
                                .city("CABA")
                                .state("BA")
                                .street("Main")
                                .number("10")
                                .build()))
                        .build())
                .zones(List.of("zone1"))
                .skills(List.of("skill1"))
                .build();

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn("ok"))) {

            CorePublisherService service = newService();
            service.sendUserCreatedToCore(response);

            RestTemplate restTemplate = mocked.constructed().get(0);
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForObject(urlCaptor.capture(), entityCaptor.capture(), eq(String.class));
            assertThat(urlCaptor.getValue()).contains("https://api.arreglacore.click/publish");

            HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
            HttpHeaders headers = entity.getHeaders();
            assertThat(headers.getFirst("x-api-key")).isEqualTo("test-key");
            assertThat(headers.getContentType()).isNotNull();

            Map<String, Object> body = entity.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("destination")).isInstanceOf(Map.class);
            Map<?, ?> destination = (Map<?, ?>) body.get("destination");
            assertThat(destination.get("eventName")).isEqualTo("user_created");

            Map<?, ?> payload = (Map<?, ?>) body.get("payload");
            assertThat(payload.get("message")).isEqualTo("Usuario creado");
            assertThat(payload.get("zones")).isEqualTo(List.of("zone1"));
            assertThat(payload.get("skills")).isEqualTo(List.of("skill1"));
            assertThat(payload.get("email")).isEqualTo("user@example.com");
        }
    }

    @Test
    void sendUserDeactivatedToCore_PublishesDeactivationEvent() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn("ok"))) {

            CorePublisherService service = newService();
            service.sendUserDeactivatedToCore(99L);

            RestTemplate restTemplate = mocked.constructed().get(0);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(String.class));
            Map<String, Object> body = entityCaptor.getValue().getBody();
            assertThat(body).isNotNull();
            Map<?, ?> destination = (Map<?, ?>) body.get("destination");
            assertThat(destination.get("eventName")).isEqualTo("user_deactivated");
            Map<?, ?> payload = (Map<?, ?>) body.get("payload");
            assertThat(payload.get("userId")).isEqualTo(99L);
        }
    }

    @Test
    void sendUserRejectedToCore_PublishesRejectedEvent() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn("ok"))) {

            CorePublisherService service = newService();
            service.sendUserRejectedToCore("user@example.com", "error");

            RestTemplate restTemplate = mocked.constructed().get(0);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(String.class));
            Map<String, Object> body = entityCaptor.getValue().getBody();
            assertThat(body).isNotNull();
            Map<?, ?> destination = (Map<?, ?>) body.get("destination");
            assertThat(destination.get("eventName")).isEqualTo("user_rejected");
            Map<?, ?> payload = (Map<?, ?>) body.get("payload");
            assertThat(payload.get("email")).isEqualTo("user@example.com");
            assertThat(String.valueOf(payload.get("message"))).contains("error");
        }
    }
}
