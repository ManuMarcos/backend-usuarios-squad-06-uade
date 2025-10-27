package com.reparaya.users.service;

import com.reparaya.users.dto.TokenValidationResponse;
import com.reparaya.users.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    private final JwtUtil jwtUtil;

    public ResponseEntity<TokenValidationResponse> validateToken(String token) {
        try {
            if (isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                log.info("Valid token for user: {} with role: {}", email, role);
                return ResponseEntity.ok(TokenValidationResponse.builder()
                        .message("Token válido")
                        .build());
            } else {
                log.warn("Invalid or expired token");
                return ResponseEntity.status(401).body(TokenValidationResponse.builder()
                        .message("Token inválido o expirado")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.status(500).body(TokenValidationResponse.builder()
                    .message("Error validando token: " + e.getMessage())
                    .build());
        }
    }

    private boolean isTokenValid(String token) {
        try {
            jwtUtil.extractAllClaims(token);
            
            if (jwtUtil.extractExpiration(token).before(new java.util.Date())) {
                log.warn("Expired token");
                return false;
            }
            
            String email = jwtUtil.extractEmail(token);
            if (email == null || email.isEmpty()) {
                log.warn("No valid email on token");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating token signature or structure: {}", e.getMessage());
            return false;
        }
    }
}
