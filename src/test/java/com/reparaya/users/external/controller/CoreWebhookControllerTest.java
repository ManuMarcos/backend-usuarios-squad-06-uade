package com.reparaya.users.external.controller;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.IncomingEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreWebhookControllerTest {

    @Mock
    private IncomingEventProcessor incomingEventProcessor;

    @InjectMocks
    private CoreWebhookController controller;

    private CoreMessage message;

    @BeforeEach
    void setUp() {
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic("users");
        destination.setEventName("alta");

        message = new CoreMessage();
        message.setMessageId("msg-123");
        message.setDestination(destination);
    }

    @Test
    void handleEventReturnsOkWhenProcessingSucceeds() {
        when(incomingEventProcessor.processEventByStrategy(message)).thenReturn(true);

        ResponseEntity<String> response = controller.handleEvent(message);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("msg-123");
    }

    @Test
    void handleEventReturnsServerErrorWhenProcessingFails() {
        when(incomingEventProcessor.processEventByStrategy(message)).thenReturn(false);

        ResponseEntity<String> response = controller.handleEvent(message);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("msg-123");
    }

    @Test
    void handleEventHandlesUnexpectedExceptions() {
        doThrow(new RuntimeException("boom"))
                .when(incomingEventProcessor).processEventByStrategy(message);

        ResponseEntity<String> response = controller.handleEvent(message);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("msg-123");
    }
}
