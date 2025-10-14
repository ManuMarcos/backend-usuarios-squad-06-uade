package com.reparaya.users.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;


@Data
public class CoreMessage {

    private String messageId;
    private OffsetDateTime timestamp;
    private String source;
    private Destination destination;
    private Map<String, Object> payload;

    @Data
    public static class Destination {
        private String channel;
        private String eventName;
    }
}