package com.smartglass.controller;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.dto.CotizacionVidrioResponse;
import com.smartglass.dto.ErrorResponse;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.VidrioSostenibleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Expone las cotizaciones de vidrio sostenible via REST.
 * CAMBIOS de esta ronda (cierre de holes):
 * - Toda la API exige sesion: sin autenticacion -> 401.
 * - POST /cotizar: el usuarioId sale de la sesion, no del body (anti-spoofing).
 * - GET /{id} y GET /usuario/{usuarioId}: solo dueno o admin (anti-IDOR).
 * - PATCH /{id}/estado: transiciones via cambiarEstado; pasar a APROBADO o
 *   RECHAZADA exige ROLE_ADMIN (403); transicion ilegal -> 409.
 * - Rutas limpias (la copia anterior tenia espacios en @PatchMapping y un
 *   "& &" que no compilaba).
 */
@RestController
@RequestMapping("/api/vidrios")
public class VidrioPersonalizadoController {

    private final VidrioSostenibleService vidrioSostenibleService;
    private final UsuarioRepository usuarioRepository;

    public VidrioPersonalizadoController(VidrioSostenibleService vidrioSostenibleService,
                                         UsuarioRepository usuarioRepository) {
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.usuarioRepository = usuarioRepository;
    }

    private Optional<Usuario> usuarioActual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName());
    }

    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizar(@RequestBody CotizacionVidrioRequest request, Authentication auth) {
        return usuarioActual(auth).<ResponseEntity<?>>map(u -> {
            request.setUsuarioId(u.getId()); // el dueno lo define la sesion, no el body
            VidrioSostenible vidrio = vidrioSostenibleService.generarCotizacion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionVidrioResponse.from(vidrio));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Debes iniciar sesion para crear una cotizacion.")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable String id, Authentication auth) {
        Optional<Usuario> u = usuarioActual(auth);
        if (u.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Autenticacion requerida."));
        }
        return vidrioSostenibleService.obtenerPorId(id)
                .filter(v -> u.get().getId().equals(v.getUsuarioId()) || PermisosAdvice.esAdmin(auth))
                .<ResponseEntity<?>>map(v -> ResponseEntity.ok(CotizacionVidrioResponse.from(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No existe una cotizacion con id " + id)));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> misCotizaciones(@PathVariable Long usuarioId, Authentication auth) {
        Optional<Usuario> u = usuarioActual(auth);
        if (u.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Autenticacion requerida."));
        }
        boolean esAdmin = PermisosAdvice.esAdmin(auth);
        boolean esDueno = u.get().getId().equals(usuarioId);
        if (!esAdmin && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("No tienes permisos para ver estas cotizaciones."));
        }
        List<CotizacionVidrioResponse> cotizaciones = vidrioSostenibleService.obtenerPorUsuario(usuarioId).stream()
                .map(CotizacionVidrioResponse::from)
                .toList();
        return ResponseEntity.ok(cotizaciones);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable String id,
                                              @RequestParam String estado,
                                              Authentication auth) {
        Optional<Usuario> u = usuarioActual(auth);
        if (u.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Autenticacion requerida."));
        }
        String objetivo = estado == null ? "" : estado.toUpperCase();
        boolean esTransicionDeGestion = VidrioSostenibleService.APROBADO.equals(objetivo)
                || VidrioSostenibleService.RECHAZADA.equals(objetivo);
        if (esTransicionDeGestion && !PermisosAdvice.esAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Solo un administrador puede gestionar el estado."));
        }
        return switch (vidrioSostenibleService.cambiarEstado(id, objetivo)) {
            case NO_ENCONTRADA -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No existe una cotizacion con id " + id));
            case TRANSICION_INVALIDA -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Transicion de estado no permitida: " + objetivo));
            case OK -> vidrioSostenibleService.obtenerPorId(id)
                    .<ResponseEntity<?>>map(v -> ResponseEntity.ok(CotizacionVidrioResponse.from(v)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("No existe una cotizacion con id " + id)));
        };
    }
}