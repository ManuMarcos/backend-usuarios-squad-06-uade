package com.reparaya.users.service;

import com.reparaya.users.entity.User;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationEmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from}")
    private String from;

    @Value("${app.frontend.base-url}")
    private String frontBaseUrl;

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl ="http://localhost:5173/verify?token=" + token;
        //String verifyUrl = frontBaseUrl + "/verify?token=" + token;

        String html = """
            <div style="font-family:Arial,sans-serif">
              <h2>Confirmá tu cuenta</h2>
              <p>Hacé clic en el botón para activar tu usuario.</p>
              <p><a href="%s" style="background:#16a34a;color:#fff;padding:10px 16px;text-decoration:none;border-radius:6px;">Verificar cuenta</a></p>
              <p style="font-size:12px;color:#555">Si no funciona, copiá y pegá este enlace en tu navegador:<br/>%s</p>
            </div>
        """.formatted(verifyUrl, verifyUrl);

        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(from)
                    .to(toEmail)
                    .subject("Confirmá tu cuenta")
                    .html(html)
                    .build();

            resend.emails().send(params);

            log.info("Verification email sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Error sending verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    public ResponseEntity<String> verifyEmail(String token) {
        String email = jwtUtil.extractEmail(token);
        Boolean ok = jwtUtil.validateToken(token, email);
        // ademas valida 'purpose'
        String purpose = jwtUtil.extractClaim(token, "purpose", String.class);
        if (!Boolean.TRUE.equals(ok) || !"verify_email".equals(purpose)) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("El usuario no existe en el sistema.");

        User u = opt.get();
        if (Boolean.TRUE.equals(u.getActive())) {
            return ResponseEntity.ok("El usuario ya estaba activo.");
        }

        u.setActive(true);
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);

        return ResponseEntity.ok("Usuario activado con éxito.");
    }
}
