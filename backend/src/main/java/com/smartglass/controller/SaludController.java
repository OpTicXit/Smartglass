package com.smartglass.controller;

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

    @GetMapping("/salud")
    public ResponseEntity<String> salud() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body("ok");
    }
}
