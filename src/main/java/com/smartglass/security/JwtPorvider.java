package com.smartglass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtPorvider corregido.
 *
 * Nota:
 * Se mantiene el nombre JwtPorvider para no romper referencias existentes.
 *
 * Mejora:
 * - Se agrega metodo para generar token incluyendo roles.
 * - Se mantiene el metodo generateToken(String) por compatibilidad.
 */
@Component
public class JwtPorvider {

    private static final Logger log = LoggerFactory.getLogger(JwtPorvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtPorvider(
            @Value("${smartglass.jwt.secret}") String secret,
            @Value("${smartglass.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Metodo compatible con el codigo actual.
     */
    public String generateToken(String username) {
        return generateToken(username, Collections.emptyList());
    }

    /**
     * Metodo recomendado para generar token con roles.
     */
    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        List<String> roles = authorities == null
                ? Collections.emptyList()
                : authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            return true;

        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT invalido: {}", ex.getMessage());
        }

        return false;
    }
}