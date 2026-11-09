package com.smartglass.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Quien abre /error a mano (sin un error de por medio) veia "Algo fallo de nuestro lado" con un 500.
 * Aqui solo llegan las peticiones normales: OncePerRequestFilter no actua en el despacho de errores,
 * asi que las paginas 4xx/5xx reales siguen saliendo igual.
 */
@Component
public class ErrorDirectoFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/error".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/home");
    }
}
