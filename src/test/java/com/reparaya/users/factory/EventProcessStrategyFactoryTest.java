package com.reparaya.users.factory;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.external.strategy.EventProcessStrategy;
import com.reparaya.users.external.strategy.EventUserDeactivateStrategy;
import com.reparaya.users.external.strategy.EventUserRegisterStrategy;
import com.reparaya.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EventProcessStrategyFactoryTest {

    @Mock
    private UserService userService;

    @Mock
    private CorePublisherService corePublisherService;

    private EventProcessStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EventProcessStrategyFactory(userService, corePublisherService);
    }

    @Test
    void getStrategy_WhenAltaPrestador_ReturnsRegisterStrategy() {
        CoreMessage event = coreMessage(EventProcessStrategyFactory.ALTA_PRESTADOR_CATALOGO);

        EventProcessStrategy strategy = factory.getStrategy(event);

        assertThat(strategy).isInstanceOf(EventUserRegisterStrategy.class);
    }

    @Test
    void getStrategy_WhenAltaUsuario_ReturnsRegisterStrategy() {
        CoreMessage event = coreMessage(EventProcessStrategyFactory.ALTA_USUARIO_BUSQUEDA);

        EventProcessStrategy strategy = factory.getStrategy(event);

        assertThat(strategy).isInstanceOf(EventUserRegisterStrategy.class);
    }

    @Test
    void getStrategy_WhenBajaPrestador_ReturnsDeactivateStrategy() {
        CoreMessage event = coreMessage(EventProcessStrategyFactory.BAJA_PRESTADOR_CATALOGO);

        EventProcessStrategy strategy = factory.getStrategy(event);

        assertThat(strategy).isInstanceOf(EventUserDeactivateStrategy.class);
    }

    @Test
    void getStrategy_WhenUnknownEvent_Throws() {
        CoreMessage event = coreMessage("unknown");

        assertThatThrownBy(() -> factory.getStrategy(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    private static CoreMessage coreMessage(String eventName) {
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setEventName(eventName);
        destination.setTopic("any");

        CoreMessage message = new CoreMessage();
        message.setDestination(destination);
        return message;
    }
}
