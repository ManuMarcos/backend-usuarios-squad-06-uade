package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.dto.UpdateUserRequest;
import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.mapper.EventMapper;
import com.reparaya.users.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.reparaya.users.mapper.EventMapper.*;
import static com.reparaya.users.util.Validators.validateRequest;

@Slf4j
@AllArgsConstructor
public class EventUserUpdateStrategy implements EventProcessStrategy {

    private UserService userService;
    private CorePublisherService corePublisherService;

    @Override
    public boolean handle(CoreMessage event) {
        log.info("Starting event user update strategy");

        UpdateUserRequest request = mapUpdateRequestFromEvent(event);
        try {
            UpdateUserResponse response = userService.updateUserPartiallyFromEvent(request);
            corePublisherService.sendUserUpdatedToCore(response);
            return true;
        } catch (Exception ex) {
            log.error("An error ocurred while processing user update though event with messageId: {} and error: {}", event.getMessageId(), ex.getMessage());
            return false;
        }
    }
}