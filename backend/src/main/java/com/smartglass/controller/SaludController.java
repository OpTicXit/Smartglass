package com.smartglass.controller;

import jakarta.servlet.http.HttpServletRequest;
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

    /* X-Ip-Vista: la IP con la que la app ve a quien pregunta (la suya propia, nada ajeno). Sirve para
       comprobar detras del proxy de Railway que el limite de solicitudes cuenta a cada visitante por separado. */
    @GetMapping("/salud")
    public ResponseEntity<String> salud(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Ip-Vista", request.getRemoteAddr()).body("ok");
    }
}
