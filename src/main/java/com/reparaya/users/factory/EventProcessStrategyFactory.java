package com.reparaya.users.factory;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.external.strategy.EventProcessStrategy;
import com.reparaya.users.external.strategy.EventUserDeactivateStrategy;
import com.reparaya.users.external.strategy.EventUserRegisterStrategy;
import com.reparaya.users.external.strategy.EventUserUpdateStrategy;
import com.reparaya.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessStrategyFactory {

    public static final String ALTA_PRESTADOR_CATALOGO = "alta_prestador";
    public static final String ALTA_USUARIO_BUSQUEDAS = "solicitado";
    public static final String MODIFICACION_PRESTADOR_CATALOGO = "modificacion_prestador";
    public static final String BAJA_PRESTADOR_CATALOGO = "baja_prestador";

    private final UserService userService;
    private final CorePublisherService corePublisherService;

    public EventProcessStrategy getStrategy(CoreMessage message) {
        String eventName = message.getDestination().getEventName();

        log.info("Getting strategy for eventName: {}", eventName);

        return switch (eventName) {
            case ALTA_PRESTADOR_CATALOGO, ALTA_USUARIO_BUSQUEDAS -> new EventUserRegisterStrategy(userService, corePublisherService);
            case MODIFICACION_PRESTADOR_CATALOGO -> new EventUserUpdateStrategy(userService);
            case BAJA_PRESTADOR_CATALOGO -> new EventUserDeactivateStrategy(userService);
            default -> throw new IllegalStateException("The eventName: " + eventName + " is not recognized.");
        };
    }
}
