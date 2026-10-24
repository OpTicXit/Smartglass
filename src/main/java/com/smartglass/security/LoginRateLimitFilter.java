package com.smartglass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * H05: limite de intentos de login por IP (fuerza bruta).
 *
 * - Cubre POST /login (formulario) y POST /auth/login (API REST). Cada intento gasta una
 *   ficha, sea correcto o no: un usuario real no inicia sesion 10 veces por minuto.
 * - Va ANTES de UsernamePasswordAuthenticationFilter: un intento bloqueado no llega a
 *   consultar la base de datos ni a comparar BCrypt.
 * - NO es un @Component (igual que RateLimitFilter): Spring Boot lo registraria tambien
 *   como filtro de servlet. Lo crea SecurityConfig. No toca las cubetas de RateLimitFilter.
 * - X-Forwarded-For solo si confiarEnForwardedFor = true (detras de un proxy propio).
 * - Memoria acotada: purga por inactividad y tope de cubetas, como RateLimitFilter.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final long VENTANA_MS = 60_000L;
    private static final long TTL_INACTIVIDAD_MS = 10 * 60_000L;
    private static final long PURGA_CADA_MS = 5 * 60_000L;
    private static final int MAX_CUBETAS = 50_000;

    private final int intentosPorMinuto;
    private final boolean confiarEnForwardedFor;

    private final ConcurrentHashMap<String, TokenBucket> cubetas = new ConcurrentHashMap<>();
    private final AtomicLong ultimaPurga = new AtomicLong(System.currentTimeMillis());

    public LoginRateLimitFilter(int intentosPorMinuto, boolean confiarEnForwardedFor) {
        this.intentosPorMinuto = intentosPorMinuto;
        this.confiarEnForwardedFor = confiarEnForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String ruta = req.getServletPath();
        return !"POST".equals(req.getMethod()) || !(ruta.equals("/login") || ruta.equals("/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long ahora = System.currentTimeMillis();
        purgarSiToca(ahora);

        TokenBucket cubeta = cubeta(ipDe(req), ahora);
        if (cubeta.tryConsume(ahora)) {
            chain.doFilter(req, res);
            return;
        }

        res.setHeader("Retry-After", String.valueOf(Math.max(1L, cubeta.segundosParaReintentar(ahora))));
        if (req.getServletPath().equals("/auth/login")) {
            res.setStatus(429);
            res.setContentType("application/json;charset=UTF-8");
            String msg = "Demasiados intentos de inicio de sesion. Espera un minuto.";
            res.getOutputStream().write(
                    ("{\"error\":\"" + msg + "\",\"mensaje\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8));
        } else {
            // Formulario HTML: volver al login con aviso (login.html lee ${param.bloqueado})
            res.sendRedirect(req.getContextPath() + "/login?bloqueado");
        }
    }

    private TokenBucket cubeta(String ip, long ahora) {
        String clave = "login|" + ip;
        TokenBucket existente = cubetas.get(clave);
        if (existente != null) return existente;
        if (cubetas.size() >= MAX_CUBETAS) {
            purgar(ahora);
            if (cubetas.size() >= MAX_CUBETAS) {
                clave = "login|desbordamiento"; // tope alcanzado: las IP nuevas comparten una cubeta
            }
        }
        return cubetas.computeIfAbsent(clave, k -> new TokenBucket(intentosPorMinuto, VENTANA_MS, ahora));
    }

    private void purgarSiToca(long ahora) {
        long previa = ultimaPurga.get();
        if (ahora - previa > PURGA_CADA_MS && ultimaPurga.compareAndSet(previa, ahora)) {
            purgar(ahora);
        }
    }

    private void purgar(long ahora) {
        cubetas.entrySet().removeIf(e -> ahora - e.getValue().getUltimoUso() > TTL_INACTIVIDAD_MS);
    }

    private String ipDe(HttpServletRequest req) {
        if (confiarEnForwardedFor) {
            String xf = req.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) {
                String primera = xf.split(",")[0].trim();
                if (!primera.isEmpty() && primera.length() <= 45) return primera;
            }
        }
        return req.getRemoteAddr();
    }
}
