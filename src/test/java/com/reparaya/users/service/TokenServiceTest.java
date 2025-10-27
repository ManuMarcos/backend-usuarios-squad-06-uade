package com.reparaya.users.service;

import com.reparaya.users.dto.TokenValidationResponse;
import com.reparaya.users.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void validateTokenReturnsOkWhenTokenIsValid() {
        String token = "valid-token";
        Claims claims = mock(Claims.class);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(jwtUtil.extractExpiration(token)).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(jwtUtil.extractEmail(token)).thenReturn("user@example.com");
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_USER");

        ResponseEntity<TokenValidationResponse> response = tokenService.validateToken(token);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage().toLowerCase()).contains("token");
    }

    @Test
    void validateTokenReturnsUnauthorizedWhenTokenInvalid() {
        String token = "invalid-token";
        Claims claims = mock(Claims.class);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(jwtUtil.extractExpiration(token)).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(jwtUtil.extractEmail(token)).thenReturn("");

        ResponseEntity<TokenValidationResponse> response = tokenService.validateToken(token);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage().toLowerCase()).contains("token");
    }

    @Test
    void validateTokenReturnsServerErrorWhenUnexpectedExceptionOccurs() {
        String token = "boom";
        Claims claims = mock(Claims.class);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(jwtUtil.extractExpiration(token)).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(jwtUtil.extractEmail(token)).thenReturn("user@example.com");
        when(jwtUtil.extractRole(token)).thenThrow(new RuntimeException("broken"));

        ResponseEntity<TokenValidationResponse> response = tokenService.validateToken(token);

        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Error validando token");
    }
}
