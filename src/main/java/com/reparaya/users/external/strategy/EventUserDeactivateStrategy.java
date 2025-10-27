package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.service.UserService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventUserDeactivateStrategy implements EventProcessStrategy {

    private UserService userService;

    @Override
    public boolean handle(CoreMessage event) {
        // To be implemented.
        return false;
    }

}
