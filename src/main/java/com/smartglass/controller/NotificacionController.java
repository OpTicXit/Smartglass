package com.smartglass.controller;

import com.smartglass.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * T3 · notificaciones por polling (la campana de las dos barras pide esto cada 60 s).
 * - GET: abierto; un visitante solo recibe las novedades del catalogo (destino TODOS).
 * - PATCH: exige cuenta (SecurityConfig responde 401 en /api/** sin sesion).
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public Map<String, Object> listar(@RequestParam(required = false) Long desde, Authentication auth) {
        return notificacionService.listar(auth, desde);
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id, Authentication auth) {
        return notificacionService.marcarLeida(auth, id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PatchMapping("/leida")
    public ResponseEntity<Void> marcarTodas(Authentication auth) {
        return notificacionService.marcarTodas(auth)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
