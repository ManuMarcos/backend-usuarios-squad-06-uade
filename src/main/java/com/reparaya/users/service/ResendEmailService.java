package com.reparaya.users.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    private final Resend resendClient;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey
    ) {
        this.resendClient = new Resend(apiKey);
    }

    public void sendResetPasswordEmail(String to, String resetUrl) {
        String html = """
                <p>Hola,</p>
                <p>Hacé clic en el siguiente enlace para restablecer tu contraseña:</p>
                <p><a href="%s">Restablecer contraseña</a></p>
                <p>Si no solicitaste este cambio, ignorá este correo.</p>
                """.formatted(resetUrl);

        CreateEmailOptions request = CreateEmailOptions.builder()
                .from("ArreglaYa <usuarios@desarrollo2-usuarios.shop>")
                .to(to)
                .subject("Olvidaste tu contraseña")
                .html(html)
                .build();

        try {
            resendClient.emails().send(request);
        } catch (ResendException e) {
            throw new RuntimeException("Error enviando email de reset de contraseña", e);
        }
    }

}