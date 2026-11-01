package com.smartglass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * F41: limitador de solicitudes por actor (usuario autenticado o IP).
 *
 * - Se aplica a /api/** y a las rutas sensibles (POST /registro, POST /auth/registro,
 *   GET /registro/disponible, POST /api/vidrios/cotizar y, solo para visitantes sin cuenta (T1),
 *   POST /vidrio-personalizado/cotizar). Las paginas HTML y los estaticos (/css, /js, /img) NO
 *   gastan fichas: una sola carga de pagina pide varios recursos y agotaria el limite.
 * - Limites por minuto: anonimo, usuario, admin y "sensible" (adicional, por actor).
 * - NO es un @Component a proposito: Spring Boot registraria el filtro dos veces (una como
 *   filtro de servlet, antes de la autenticacion). Lo crea SecurityConfig y lo coloca
 *   despues de UsernamePasswordAuthenticationFilter, cuando ya hay usuario autenticado.
 * - X-Forwarded-For solo se usa si confiarEnForwardedFor = true (detras de un proxy propio);
 *   si no, cualquiera podria cambiar de "IP" a voluntad o llenar la memoria con claves falsas.
 * - Memoria acotada: purga por inactividad y tope de cubetas.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long VENTANA_MS = 60_000L;
    private static final long TTL_INACTIVIDAD_MS = 10 * 60_000L;
    private static final long PURGA_CADA_MS = 5 * 60_000L;
    private static final int MAX_CUBETAS = 100_000;

    private final int limiteAnonimo;
    private final int limiteUsuario;
    private final int limiteAdmin;
    private final int limiteSensible;
    private final boolean confiarEnForwardedFor;

    private final ConcurrentHashMap<String, TokenBucket> cubetas = new ConcurrentHashMap<>();
    private final AtomicLong ultimaPurga = new AtomicLong(System.currentTimeMillis());

    public RateLimitFilter(int limiteAnonimo, int limiteUsuario, int limiteAdmin,
                           int limiteSensible, boolean confiarEnForwardedFor) {
        this.limiteAnonimo = limiteAnonimo;
        this.limiteUsuario = limiteUsuario;
        this.limiteAdmin = limiteAdmin;
        this.limiteSensible = limiteSensible;
        this.confiarEnForwardedFor = confiarEnForwardedFor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // getServletPath() no incluye parametros de ruta (;jsessionid=...) que permitirian esquivar la regla
        String ruta = req.getServletPath();
        boolean api = ruta.startsWith("/api/");
        boolean disponibilidad = ruta.equals("/registro/disponible");
        // H11: POST /auth/registro (API REST) entra en el mismo bucket sensible que POST /registro
        boolean registroApi = "POST".equals(req.getMethod()) && ruta.equals("/auth/registro");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonimo = auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
        // T1 (D4): el configurador HTML guarda obras sin cuenta; un visitante entra en el bucket sensible
        boolean cotizacionAnonima = anonimo && "POST".equals(req.getMethod())
                && ruta.equals("/vidrio-personalizado/cotizar");
        boolean sensible = disponibilidad || registroApi || cotizacionAnonima
                || ("POST".equals(req.getMethod()) && (ruta.equals("/api/vidrios/cotizar") || ruta.equals("/registro")));

        if (!api && !sensible) {
            chain.doFilter(req, res);
            return;
        }

        boolean admin = !anonimo && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        String actor = anonimo ? "ip:" + ipDe(req) : "u:" + auth.getName();

        long ahora = System.currentTimeMillis();
        purgarSiToca(ahora);

        int limite = admin ? limiteAdmin : (anonimo ? limiteAnonimo : limiteUsuario);
        TokenBucket general = cubeta("g|" + actor, limite, ahora);
        TokenBucket especifica = sensible ? cubeta("s|" + actor, limiteSensible, ahora) : null;

        boolean pasa = general.tryConsume(ahora) && (especifica == null || especifica.tryConsume(ahora));
        if (pasa) {
            chain.doFilter(req, res);
            return;
        }

        long espera = general.segundosParaReintentar(ahora);
        if (especifica != null) espera = Math.max(espera, especifica.segundosParaReintentar(ahora));
        res.setHeader("Retry-After", String.valueOf(Math.max(1L, espera)));

        if (api || disponibilidad || registroApi) {
            res.setStatus(429);
            res.setContentType("application/json;charset=UTF-8");
            String msg = "Demasiadas solicitudes. Intenta de nuevo en un minuto.";
            res.getOutputStream().write(
                    ("{\"error\":\"" + msg + "\",\"mensaje\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8));
        } else if (cotizacionAnonima) {
            res.sendRedirect(req.getContextPath() + "/vidrio-personalizado/cotizar?limite");
        } else {
            // POST /registro desde el formulario HTML: volver al formulario con aviso (no Whitelabel)
            res.sendRedirect(req.getContextPath() + "/registro?limite");
        }
    }

    private TokenBucket cubeta(String clave, int limite, long ahora) {
        TokenBucket existente = cubetas.get(clave);
        if (existente != null) return existente;
        if (cubetas.size() >= MAX_CUBETAS) {
            purgar(ahora);
            if (cubetas.size() >= MAX_CUBETAS) {
                // Tope alcanzado: las claves nuevas comparten una cubeta de desbordamiento
                clave = "desbordamiento|" + clave.charAt(0);
            }
        }
        return cubetas.computeIfAbsent(clave, k -> new TokenBucket(limite, VENTANA_MS, ahora));
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
