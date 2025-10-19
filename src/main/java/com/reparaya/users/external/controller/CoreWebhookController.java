package com.reparaya.users.external.controller;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.IncomingEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/core-usuarios")
@Slf4j
@RequiredArgsConstructor
public class CoreWebhookController {

    private final IncomingEventProcessor incomingEventProcessor;

    @PostMapping
    public ResponseEntity<String> handleEvent(@RequestBody CoreMessage message) {
        log.info("Event received {} from {} ", message.getDestination().getEventName(), message.getSource());
        // send ack
        boolean processed = incomingEventProcessor.processEventByStrategy(message);
        if (processed) {
            // send event to core
            return ResponseEntity.ok("Evento " + message.getMessageId() + " procesado de forma exitosa");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ocurrió un error al procesar el evento " + message.getMessageId());
    }

}
