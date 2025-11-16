package com.reparaya.users.external.strategy;

import com.reparaya.users.dto.CoreMessage;

public interface EventProcessStrategy {

    boolean handle(CoreMessage event);

}
