package com.reparaya.users.external.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class EventUserRegisterStrategy implements EventProcessStrategy {

    private UserService userService;

    @Override
    public boolean handle(CoreMessage event) {
        RegisterRequest request = mapRegisterRequestFromEvent(event);
        try {
            userService.registerUser(request);
            return true;
        } catch (Exception ex) {
            log.error("An error ocurred while processing user registration though event with messageId {} and error {}", event.getMessageId(), ex.getMessage());
            return false;
        }
    }

    private RegisterRequest mapRegisterRequestFromEvent(CoreMessage event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.convertValue(event.getPayload(), RegisterRequest.class);
        } catch (Exception ex) {
            log.error("An error ocurred while deserializing event {}. Error: {}", event.getMessageId(), ex.getMessage());
            throw ex;
        }
    }
}
