package com.smartglass.controller;

import com.smartglass.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T6 · Chequeo de salud para Render y Railway (healthCheckPath = /salud).
 * Responde 200 en cuanto el servidor atiende, sin consultar las bases: si MySQL o Mongo
 * tardan, la plataforma no reinicia el contenedor en bucle. Publico en SecurityConfig.
 */
@RestController
public class SaludController {

    private final boolean confiarForwardedFor;

    public SaludController(@Value("${smartglass.ratelimit.confiar-forwarded-for:false}") boolean confiarForwardedFor) {
        this.confiarForwardedFor = confiarForwardedFor;
    }

    /* X-Ip-Vista: la IP con la que el limite de solicitudes cuenta a quien pregunta (la suya propia, nada ajeno).
       Detras del proxy de Railway debe ser la IP publica del visitante, no la del proxy. */
    @GetMapping("/salud")
    public ResponseEntity<String> salud(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Ip-Vista", RateLimitFilter.claveDeIp(request, confiarForwardedFor)).body("ok");
    }
}
