package com.reparaya.users.security;

import com.reparaya.users.service.UserService;
import com.reparaya.users.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;

    @MockBean UserService userService;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void getUsers_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void getUsers_withClientToken_returns403() throws Exception {
        String token = jwtUtil.generateToken("client@example.com", "CLIENTE");

        mvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUsers_withAdminToken_returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");

        mvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        verify(userService).getAllUsers();
    }

    @Test
    void getUsers_withExpiredToken_returns401() throws Exception {
        String secret = (String) ReflectionTestUtils.getField(jwtUtil, "secret");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .subject("expired@example.com")
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(10)))
                .signWith(key)
                .compact();

        mvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
