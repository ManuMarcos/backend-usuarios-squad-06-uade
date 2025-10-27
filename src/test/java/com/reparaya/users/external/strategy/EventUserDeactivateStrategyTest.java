package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventUserDeactivateStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private CorePublisherService corePublisherService;

    @InjectMocks
    private EventUserDeactivateStrategy strategy;

    @Test
    void handle_WhenDeactivationSucceeds_SendsNotification() {
        CoreMessage event = buildEventWithId(42L);

        boolean result = strategy.handle(event);

        assertThat(result).isTrue();
        verify(userService).deactivateUserFromEvent(42L);
        verify(corePublisherService).sendUserDeactivatedToCore(42L);
    }

    @Test
    void handle_WhenDeactivationFails_ReturnsFalse() {
        CoreMessage event = buildEventWithId(50L);
        doThrow(new RuntimeException("boom")).when(userService).deactivateUserFromEvent(50L);

        boolean result = strategy.handle(event);

        assertThat(result).isFalse();
        verify(corePublisherService, never()).sendUserDeactivatedToCore(anyLong());
    }

    @Test
    void handle_WhenPayloadMissingId_ThrowsIllegalArgumentException() {
        CoreMessage event = new CoreMessage();
        event.setMessageId("id");
        event.setPayload(Map.of());

        assertThatThrownBy(() -> strategy.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event id: id");
    }

    private static CoreMessage buildEventWithId(long id) {
        CoreMessage event = new CoreMessage();
        event.setPayload(Map.of("id", id));
        return event;
    }
}
