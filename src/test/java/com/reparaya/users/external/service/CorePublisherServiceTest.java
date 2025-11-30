package com.reparaya.users.external.service;

import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.dto.UserDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CorePublisherServiceTest {

    private HttpServer server;
    private List<String> receivedBodies;
    private List<String> receivedKeys;
    private AtomicInteger calls;

    @BeforeEach
    void startServer() throws IOException {
        receivedBodies = new ArrayList<>();
        receivedKeys = new ArrayList<>();
        calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/publish", new TestHandler());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendUserCreatedToCore_postsEventWithApiKey() {
        CorePublisherService service = new CorePublisherService();
        String url = "http://localhost:" + server.getAddress().getPort() + "/publish";
        ReflectionTestUtils.setField(service, "coreEventPublishUrl", url);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        RegisterResponse response = RegisterResponse.builder()
                .message("ok")
                .user(UserDto.builder()
                        .email("user@example.com")
                        .role("ROLE_USER")
                        .build())
                .build();

        service.sendUserCreatedToCore(response);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(receivedKeys).contains("test-key");
        assertThat(String.join(" ", receivedBodies)).contains("user_created").contains("user@example.com");
    }

    @Test
    void sendUserUpdatedAndRejectedToCore_postsDistinctEvents() {
        CorePublisherService service = new CorePublisherService();
        String url = "http://localhost:" + server.getAddress().getPort() + "/publish";
        ReflectionTestUtils.setField(service, "coreEventPublishUrl", url);
        ReflectionTestUtils.setField(service, "apiKey", "another-key");

        UpdateUserResponse update = UpdateUserResponse.builder()
                .user(UserDto.builder().email("upd@example.com").build())
                .build();

        service.sendUserUpdatedToCore(update);
        service.sendUserRejectedToCore("rej@example.com", "bad data");

        assertThat(calls.get()).isEqualTo(2);
        assertThat(receivedKeys).allMatch("another-key"::equals);
        String body = String.join(" ", receivedBodies);
        assertThat(body).contains("user_updated").contains("user_rejected").contains("rej@example.com");
    }

    @Test
    void sendUserDeactivatedToCore_postsDeactivateEvent() {
        CorePublisherService service = new CorePublisherService();
        String url = "http://localhost:" + server.getAddress().getPort() + "/publish";
        ReflectionTestUtils.setField(service, "coreEventPublishUrl", url);
        ReflectionTestUtils.setField(service, "apiKey", "key");

        service.sendUserDeactivatedToCore(55L);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(receivedBodies.getFirst()).contains("user_deactivated").contains("55");
    }

    private class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            calls.incrementAndGet();
            receivedKeys.add(exchange.getRequestHeaders().getFirst("x-api-key"));
            try (InputStream is = exchange.getRequestBody()) {
                receivedBodies.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }
    }
}
