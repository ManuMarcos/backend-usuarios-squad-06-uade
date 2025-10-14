package com.reparaya.users.external.controller;

import com.reparaya.users.dto.CoreMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/core-usuarios")
@Slf4j
public class CoreWebhookController {

    @PostMapping
    public ResponseEntity<String> handleEvent(@RequestBody CoreMessage message) {
        log.info("Recibido evento desde " + message.getSource());
        return ResponseEntity.ok("test");
    }

}
