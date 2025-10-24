package com.reparaya.users.external.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.reparaya.users.mapper.EventMapper.mapRegisterRequestFromEvent;
import static com.reparaya.users.util.Validators.validateRequest;

@Slf4j
@AllArgsConstructor
public class EventUserRegisterStrategy implements EventProcessStrategy {

    private UserService userService;
    private CorePublisherService corePublisherService;

    @Override
    public boolean handle(CoreMessage event) {
        log.info("Starting event user register strategy");
        RegisterRequest request = mapRegisterRequestFromEvent(event);
        try {
            validateRequest(request);
            userService.registerUser(request);
            return true;
        } catch (Exception ex) {
            log.error("An error ocurred while processing user registration though event with messageId: {} and error: {}", event.getMessageId(), ex.getMessage());
            corePublisherService.sendUserRejectedToCore(event, ex.getMessage());
            return false;
        }
    }

}
