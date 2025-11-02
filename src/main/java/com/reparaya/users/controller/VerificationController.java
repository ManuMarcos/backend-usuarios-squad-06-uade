package com.reparaya.users.controller;

import com.reparaya.users.entity.User;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.service.UserService;
import com.reparaya.users.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Verification", description = "Confirmación de cuenta de usuario")
public class VerificationController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;


    @Operation(summary = "Confirma el email del usuario (público, sin JWT)")
    @GetMapping("/verify-email")

    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        //validar estructura y expiración
        String email = jwtUtil.extractEmail(token); // o extractSubject(token)
        Boolean ok = jwtUtil.validateToken(token, email);
        // ademas valida 'purpose'
        String purpose = jwtUtil.extractClaim(token, "purpose", String.class);
        if (!Boolean.TRUE.equals(ok) || !"verify_email".equals(purpose)) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Usuario no encontrado");

        User u = opt.get();
        if (Boolean.TRUE.equals(u.getActive())) {
            return ResponseEntity.ok("Usuario ya estaba activo");
        }

        u.setActive(true);
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);

        //Responder 200 (opcional: redirigir a /verified del front con 302) TBD
        return ResponseEntity.ok("Usuario activado");
    }

    @PatchMapping("/verify-email")
    public ResponseEntity<String> verifyEmailPatch(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Falta token");
        }

        // 1) validar firma/exp
        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o expirado");
        }

        // 2) validar proposito
        String purpose = jwtUtil.extractClaim(token, "purpose", String.class);
        if (!"verify_email".equals(purpose)) {
            return ResponseEntity.badRequest().body("Token con propósito inválido");
        }

        // 3) activar por email
        String email = jwtUtil.extractEmail(token);
        boolean activated = userService.activateUserByEmailIfNeeded(email);

        if (activated) return ResponseEntity.ok("Cuenta verificada con éxito");
        return ResponseEntity.ok("La cuenta ya estaba verificada");
    }

}
