package com.reparaya.users.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventMapper {

    public static RegisterRequest mapRegisterRequestFromEvent(CoreMessage event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            RegisterRequest registerRequest = mapper.convertValue(event.getPayload(), RegisterRequest.class);
            // TODO: set origin
            return registerRequest;
        } catch (Exception ex) {
            log.error("An error ocurred while deserializing event with messageId: {}. Error: {}", event.getMessageId(), ex.getMessage());
            throw ex;
        }
    }
}
