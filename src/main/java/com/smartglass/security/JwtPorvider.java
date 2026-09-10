package com.smartglass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * VERSION AJUSTADA A JJWT 0.12.x: tu pom.xml quedo con esa version
 * (no 0.11.5 como asumi originalmente), que renombro/elimino varios
 * metodos: Jwts.parserBuilder() -> Jwts.parser(); parseClaimsJws()
 * -> parseSignedClaims(); getBody() -> getPayload(); y signWith(Key)
 * ya no necesita el SignatureAlgorithm explicito (lo infiere de la
 * clave). Si prefieres mantener el codigo con la API vieja, fija
 * jjwt a la version 0.11.5 en el pom.xml en vez de usar esta clase.
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

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
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