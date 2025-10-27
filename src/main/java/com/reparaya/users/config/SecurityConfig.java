package com.reparaya.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                        "/api/users/register",
                        "/api/users/login",
                        "/api/users/*/reset-password",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webhook/**",
                        "/api/token/validate"
                ).permitAll()
                // Endpoints de permisos - ver permisos (todos los roles)
                .requestMatchers("/api/permissions/user/**").hasAnyRole("ADMIN", "PRESTADOR", "CLIENTE")
                .requestMatchers("/api/permissions/all", "/api/permissions/module/**").hasAnyRole("ADMIN", "PRESTADOR", "CLIENTE")
                // Endpoints de permisos - gestión (solo ADMIN)
                .requestMatchers("/api/permissions/user/*/add", "/api/permissions/user/*/remove", "/api/permissions/user/*/sync").hasRole("ADMIN")
                // Endpoints de usuarios - gestión (solo ADMIN)
                .requestMatchers("/api/users").hasRole("ADMIN")
                .requestMatchers("/api/users/*/permissions/**").hasAnyRole("ADMIN", "PRESTADOR", "CLIENTE")
                .requestMatchers("/api/users/*/assign-role").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
