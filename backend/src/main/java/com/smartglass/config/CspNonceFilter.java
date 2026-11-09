package com.smartglass.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Content-Security-Policy con nonce por peticion. Solo corren los scripts servidos por SmartGlass
 * ('self') y los <script> en linea de las plantillas, que llevan th:attr="nonce=${cspNonce}".
 * Un script inyectado (XSS) no conoce el nonce y el navegador no lo ejecuta.
 * Los estilos no se limitan (hay style="" en las plantillas y en el JS).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CspNonceFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO = "cspNonce";
    /** Selector de pais del telefono en el registro (intl-tel-input: script, utils y estilos) */
    private static final String CDN = "https://cdn.jsdelivr.net";
    /** Referencia externa de resistencia que consulta el detalle de la obra (vidrio-personalizado-detalle.html) */
    private static final String API_RESISTENCIA = "https://api-production-d556.up.railway.app";
    private final SecureRandom azar = new SecureRandom();

    static String politica(String nonce) {
        return "default-src 'self'; script-src 'self' 'nonce-" + nonce + "' " + CDN + "; object-src 'none'; base-uri 'self'; "
                + "frame-ancestors 'none'; form-action 'self' https://accounts.google.com; "
                + "img-src 'self' data: blob: https:; style-src 'self' 'unsafe-inline' " + CDN + "; font-src 'self' data:; "
                + "connect-src 'self' " + API_RESISTENCIA + "; worker-src 'self' blob:";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] b = new byte[16];
        azar.nextBytes(b);
        String nonce = Base64.getEncoder().encodeToString(b);
        request.setAttribute(ATRIBUTO, nonce);
        response.setHeader("Content-Security-Policy", politica(nonce));
        chain.doFilter(request, response);
    }
}
