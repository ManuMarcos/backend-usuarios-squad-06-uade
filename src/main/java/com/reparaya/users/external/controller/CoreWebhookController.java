package com.reparaya.users.external.controller;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.external.service.CorePublisherService;
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
    private final CorePublisherService corePublisherService;

    @PostMapping
    public ResponseEntity<String> handleEvent(@RequestBody CoreMessage message) {
        log.info("Event received {} from {} ", message.getDestination().getEventName(), message.getSource());
        try {
            // TODO: chequear si esta forma de ack esta bien...
            boolean processed = incomingEventProcessor.processEventByStrategy(message);
            corePublisherService.sendAckToCore(message.getMessageId());
            if (processed) {
                // send event to core
                return ResponseEntity.ok("Evento " + message.getMessageId() + " procesado de forma exitosa");
            }
        } catch (Exception ex) {
            log.error("Unexpected error while processing event with messageId {}. Error {}", message.getMessageId(), ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ocurrió un error al procesar el evento " + message.getMessageId());
    }

}
