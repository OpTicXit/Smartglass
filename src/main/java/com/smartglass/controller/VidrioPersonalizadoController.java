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

/**
 * Expone las cotizaciones de vidrio sostenible via REST.
 * CAMBIOS (parche 2 + hallazgo de seguridad):
 *  - PATCH /{id}/estado: pasar a APROBADO exige ROLE_ADMIN; sin rol, 403.
 *    (Cierra la puerta trasera: hoy /api/vidrios/** cae en permitAll.)
 *  - GET /usuario/{usuarioId}: solo el dueno o un admin (cierra el IDOR).
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

    @PostMapping("/cotizar")
    public ResponseEntity<CotizacionVidrioResponse> cotizar(@RequestBody CotizacionVidrioRequest request) {
        VidrioSostenible vidrio = vidrioSostenibleService.generarCotizacion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionVidrioResponse.from(vidrio));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable String id) {
        return vidrioSostenibleService.obtenerPorId(id)
                .<ResponseEntity<?>>map(vidrio -> ResponseEntity.ok(CotizacionVidrioResponse.from(vidrio)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No existe una cotizacion con id " + id)));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> misCotizaciones(@PathVariable Long usuarioId, Authentication auth) {
        boolean esAdmin = PermisosAdvice.esAdmin(auth);
        boolean esDueno = usuarioRepository.findByUsername(auth.getName())
                .map(Usuario::getId)
                .filter(id -> id.equals(usuarioId))
                .isPresent();
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
        if ("APROBADO".equalsIgnoreCase(estado) && !PermisosAdvice.esAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Solo un administrador puede aprobar cotizaciones."));
        }
        return vidrioSostenibleService.actualizarEstado(id, estado)
                .<ResponseEntity<?>>map(vidrio -> ResponseEntity.ok(CotizacionVidrioResponse.from(vidrio)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No existe una cotizacion con id " + id)));
    }
}