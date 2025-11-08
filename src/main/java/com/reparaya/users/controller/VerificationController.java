package com.reparaya.users.controller;

import com.reparaya.users.service.VerificationEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Verification", description = "Confirmación de cuenta de usuario")
public class VerificationController {

    private final VerificationEmailService emailService;

    @Operation(summary = "Confirma el email del usuario (público, sin JWT)")
    @PatchMapping("/verify-email")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        return emailService.verifyEmail(token);
    }

}
