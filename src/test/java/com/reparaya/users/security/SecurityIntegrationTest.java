package com.reparaya.users.security;

import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import com.reparaya.users.util.RegisterOriginEnum;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @BeforeEach
    void setUpData() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(Role.builder()
                .name("ROLE_ADMIN")
                .description("Administrador")
                .active(true)
                .build());

        Role clientRole = roleRepository.save(Role.builder()
                .name("ROLE_CLIENTE")
                .description("Cliente")
                .active(true)
                .build());

        List<User> seedUsers = List.of(
                User.builder()
                        .email("admin@example.com")
                        .firstName("Admin")
                        .lastName("User")
                        .role(adminRole)
                        .active(true)
                        .registerOrigin(RegisterOriginEnum.WEB_USUARIOS.name())
                        .build(),
                User.builder()
                        .email("client@example.com")
                        .firstName("Client")
                        .lastName("User")
                        .role(clientRole)
                        .active(true)
                        .registerOrigin(RegisterOriginEnum.WEB_USUARIOS.name())
                        .build()
        );

        seedUsers.forEach(userRepository::save);
    }

    @AfterEach
    void cleanData() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void getUsers_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_withClientToken_returns403() throws Exception {
        String token = jwtUtil.generateToken("client@example.com", "CLIENTE");

        mvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_withAdminToken_returns200() throws Exception {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");

        mvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
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
    }
}
