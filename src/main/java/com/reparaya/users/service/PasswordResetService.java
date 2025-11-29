package com.reparaya.users.service;

import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.entity.PasswordResetToken;
import com.reparaya.users.entity.User;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.repository.PasswordResetTokenRepository;
import com.reparaya.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ResendEmailService emailService;
    private final LdapUserService ldapUserService;
    private final CorePublisherService corePublisherService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Genera token y envía email. Si el usuario no existe,
     * no revela nada (simplemente no hace nada).
     */
    @Transactional
    public void createAndSendResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(expiry);
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + encodedToken;

        emailService.sendResetPasswordEmail(user.getEmail(), resetUrl);
    }

    /**
     * Lógica para usar desde el endpoint que recibe newPassword + token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado o ya utilizado");
        }

        User user = resetToken.getUser();
        ldapUserService.resetUserPassword(user, newPassword);
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

}
