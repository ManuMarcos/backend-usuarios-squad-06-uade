package com.reparaya.users.service;

import com.reparaya.users.entity.PasswordResetToken;
import com.reparaya.users.entity.User;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.repository.PasswordResetTokenRepository;
import com.reparaya.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private ResendEmailService emailService;
    @Mock private LdapUserService ldapUserService;
    @Mock private CorePublisherService corePublisherService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "https://app.test");
    }

    @Test
    void createAndSendResetToken_doesNothingWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.createAndSendResetToken("missing@example.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendResetPasswordEmail(any(), any());
    }

    @Test
    void createAndSendResetToken_savesTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordResetService.createAndSendResetToken("user@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.isUsed()).isFalse();

        verify(emailService).sendResetPasswordEmail(eq("user@example.com"), contains("https://app.test"));
    }

    @Test
    void resetPassword_marksTokenUsedAndCallsLdap() {
        User user = new User();
        user.setEmail("user@example.com");
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        passwordResetService.resetPassword("abc", "NewSecret1!");

        assertThat(token.isUsed()).isTrue();
        verify(ldapUserService).resetUserPassword(user, "NewSecret1!");
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_throwsWhenTokenExpiredOrUsed() {
        PasswordResetToken expired = new PasswordResetToken();
        expired.setToken("expired");
        expired.setUser(new User());
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expired.setUsed(false);
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token expirado");

        PasswordResetToken used = new PasswordResetToken();
        used.setToken("used");
        used.setUser(new User());
        used.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        used.setUsed(true);
        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
