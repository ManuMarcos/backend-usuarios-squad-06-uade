package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.UpdateUserRequest;
import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.reparaya.users.mapper.EventMapper.TOPIC_USUARIO_MODULE_SEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventUserUpdateStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private CorePublisherService corePublisherService;

    @InjectMocks
    private EventUserUpdateStrategy strategy;

    private CoreMessage message;

    @BeforeEach
    void setUp() {
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic(TOPIC_USUARIO_MODULE_SEARCH);
        destination.setEventName("modificacion");

        message = new CoreMessage();
        message.setMessageId("msg-123");
        message.setDestination(destination);
        message.setPayload(Map.of(
                "userId", 25,
                "email", "new@example.com",
                "firstName", "John"
        ));
    }

    @Test
    void handleUpdatesUserAndPublishesResponse() {
        UpdateUserResponse response = UpdateUserResponse.builder().build();
        when(userService.updateUserPartiallyFromEvent(any(UpdateUserRequest.class)))
                .thenReturn(response);

        boolean result = strategy.handle(message);

        assertThat(result).isTrue();
        ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
        verify(userService).updateUserPartiallyFromEvent(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        verify(corePublisherService).sendUserUpdatedToCore(response);
    }

    @Test
    void handleReturnsFalseWhenServiceFails() {
        when(userService.updateUserPartiallyFromEvent(any(UpdateUserRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        boolean result = strategy.handle(message);

        assertThat(result).isFalse();
        verify(corePublisherService, never()).sendUserUpdatedToCore(any());
    }
}
