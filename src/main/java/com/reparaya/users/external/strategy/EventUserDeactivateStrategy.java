package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.reparaya.users.mapper.EventMapper.getUserIdFromDeactivateUserEvent;

@Slf4j
@AllArgsConstructor
public class EventUserDeactivateStrategy implements EventProcessStrategy {

    private UserService userService;
    private CorePublisherService corePublisherService;

    @Override
    public boolean handle(CoreMessage event) {
        log.info("Starting event user deactivate strategy");
        Long userId = Long.valueOf(getUserIdFromDeactivateUserEvent(event));
        try {
            userService.deactivateUserFromEvent(userId);
            corePublisherService.sendUserDeactivatedToCore(userId);
            return true;
        } catch (Exception ex) {
            log.error("An error ocurred while deactivating user id: {} for message id: {}", userId, event.getMessageId());
            return false;
        }
    }

}
