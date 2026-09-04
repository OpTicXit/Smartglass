package com.smartglass.controller;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.dto.CotizacionVidrioResponse;
import com.smartglass.dto.ErrorResponse;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.service.VidrioSostenibleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expone las cotizaciones de vidrio sostenible via REST.
 * No conoce Mongo, ni formulas de CO2/energia: solo traduce
 * HTTP <-> DTOs y delega en VidrioSostenibleService (persistencia)
 * que a su vez delega en AnalisisService (matematica).
 */
@RestController
@RequestMapping("/api/vidrios")
public class VidrioPersonalizadoController {

    private final VidrioSostenibleService vidrioSostenibleService;

    public VidrioPersonalizadoController(VidrioSostenibleService vidrioSostenibleService) {
        this.vidrioSostenibleService = vidrioSostenibleService;
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
    public ResponseEntity<List<CotizacionVidrioResponse>> misCotizaciones(@PathVariable Long usuarioId) {
        List<CotizacionVidrioResponse> cotizaciones = vidrioSostenibleService.obtenerPorUsuario(usuarioId).stream()
                .map(CotizacionVidrioResponse::from)
                .toList();
        return ResponseEntity.ok(cotizaciones);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable String id, @RequestParam String estado) {
        return vidrioSostenibleService.actualizarEstado(id, estado)
                .<ResponseEntity<?>>map(vidrio -> ResponseEntity.ok(CotizacionVidrioResponse.from(vidrio)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No existe una cotizacion con id " + id)));
    }
}