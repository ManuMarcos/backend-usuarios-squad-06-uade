package com.reparaya.users.controller;

import com.reparaya.users.dto.TokenValidationRequest;
import com.reparaya.users.dto.TokenValidationResponse;
import com.reparaya.users.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TokenController tokenController;

    @Test
    void validateTokenDelegatesServiceResponse() {
        String token = "jwt-token";
        TokenValidationRequest request = TokenValidationRequest.builder()
                .token(token)
                .build();
        TokenValidationResponse payload = TokenValidationResponse.builder()
                .message("ok")
                .build();
        ResponseEntity<TokenValidationResponse> expectedResponse = ResponseEntity.ok(payload);

        when(tokenService.validateToken(token)).thenReturn(expectedResponse);

        ResponseEntity<TokenValidationResponse> response = tokenController.validateToken(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(tokenService).validateToken(token);
    }
}
