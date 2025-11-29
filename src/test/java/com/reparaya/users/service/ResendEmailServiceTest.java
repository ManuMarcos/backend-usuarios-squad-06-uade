package com.reparaya.users.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.Emails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    @Mock private Resend resendClient;
    @Mock private Emails emails;

    private ResendEmailService resendEmailService;

    @BeforeEach
    void setUp() {
        resendEmailService = new ResendEmailService("dummy-key");
        ReflectionTestUtils.setField(resendEmailService, "resendClient", resendClient);
        when(resendClient.emails()).thenReturn(emails);
    }

    @Test
    void sendResetPasswordEmail_sendsEmail() throws Exception {
        resendEmailService.sendResetPasswordEmail("to@example.com", "https://reset");

        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    void sendResetPasswordEmail_wrapsResendExceptions() throws Exception {
        doThrow(new ResendException("boom", null)).when(emails).send(any());

        assertThatThrownBy(() -> resendEmailService.sendResetPasswordEmail("to@example.com", "https://reset"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error enviando email");
    }
}
