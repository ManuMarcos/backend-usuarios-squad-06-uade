package com.reparaya.users.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 horas por defecto
    private Long expiration;

    private SecretKey getSigningKey() {
        // Si el secret parece Base64, lo decodificamos; si no, usamos bytes “raw”.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignore) {
            keyBytes = secret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes); // Requiere >= 32 bytes para HS256
    }

    public String generateEmailVerificationToken(String email, long ttlMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "verify_email");
        return generateTokenWithClaims(claims, email, ttlMillis);
    }

    public boolean validateEmailVerificationToken(String token, String expectedEmail) {
        try {
            Claims claims = extractAllClaims(token);

            // 1) propósito correcto
            String purpose = claims.get("purpose", String.class);
            if (!"verify_email".equals(purpose)) return false;

            // 2) subject = email esperado
            if (!expectedEmail.equals(claims.getSubject())) return false;

            // 3) no expirado
            Date exp = claims.getExpiration();
            if (exp == null || exp.before(new Date())) return false;

            return true;
        } catch (Exception e) {
            log.error("Token de verificación inválido: {}", e.getMessage());
            return false;
        }
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }




    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            return (extractedEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }

    public String generateTokenWithClaims(Map<String, Object> claims, String subject, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)          // subject = email (o userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey()) // clave ya configurada en jwt.secret
                .compact();
    }

    /** Helper para extraer un claim genérico */
    public <T> T extractClaim(String token, String claimName, Class<T> clazz) {
        Claims claims = extractAllClaims(token);
        Object val = claims.get(claimName);
        return clazz.isInstance(val) ? clazz.cast(val) : null;
    }

}
