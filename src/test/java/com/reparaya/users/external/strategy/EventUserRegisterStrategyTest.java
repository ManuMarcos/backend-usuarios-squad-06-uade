package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.mapper.EventMapper;
import com.reparaya.users.service.UserService;
import com.reparaya.users.util.Validators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventUserRegisterStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private CorePublisherService corePublisherService;

    @Test
    void handle_WhenValidationAndRegistrationSucceed_ReturnsTrue() {
        EventUserRegisterStrategy strategy = new EventUserRegisterStrategy(userService, corePublisherService);
        CoreMessage event = new CoreMessage();
        event.setMessageId("msg");
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .build();

        try (MockedStatic<EventMapper> mapperMock = mockStatic(EventMapper.class);
             MockedStatic<Validators> validatorsMock = mockStatic(Validators.class)) {
            mapperMock.when(() -> EventMapper.mapRegisterRequestFromEvent(event)).thenReturn(request);
            validatorsMock.when(() -> Validators.validateRequest(request)).thenAnswer(invocation -> null);

            boolean result = strategy.handle(event);

            assertThat(result).isTrue();
            verify(userService).registerUserFromEvent(request, event);
            verify(corePublisherService, never()).sendUserRejectedToCore(anyString(), anyString());
        }
    }

    @Test
    void handle_WhenRegistrationFails_SendsRejectedAndReturnsFalse() {
        EventUserRegisterStrategy strategy = new EventUserRegisterStrategy(userService, corePublisherService);
        CoreMessage event = new CoreMessage();
        event.setMessageId("msg");
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .build();

        try (MockedStatic<EventMapper> mapperMock = mockStatic(EventMapper.class);
             MockedStatic<Validators> validatorsMock = mockStatic(Validators.class)) {
            mapperMock.when(() -> EventMapper.mapRegisterRequestFromEvent(event)).thenReturn(request);
            validatorsMock.when(() -> Validators.validateRequest(request)).thenAnswer(invocation -> null);
            doThrow(new RuntimeException("boom")).when(userService).registerUserFromEvent(request, event);

            boolean result = strategy.handle(event);

            assertThat(result).isFalse();
            verify(corePublisherService).sendUserRejectedToCore("user@example.com", "boom");
        }
    }
}
