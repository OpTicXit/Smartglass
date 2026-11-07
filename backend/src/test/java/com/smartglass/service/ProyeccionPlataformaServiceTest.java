package com.smartglass.service;

import com.smartglass.model.mongo.HistorialActividad;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.HistorialActividadRepository;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProyeccionPlataformaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 7);

    private final HistorialActividadRepository historial = mock(HistorialActividadRepository.class);
    private final VidrioSostenibleRepository vidrios = mock(VidrioSostenibleRepository.class);

    private static HistorialActividad dia(LocalDate fecha, int cotizaciones, double co2) {
        return new HistorialActividad(fecha, cotizaciones, co2, "demo");
    }

    private static VidrioSostenible obra(LocalDateTime fecha, double co2) {
        VidrioSostenible v = new VidrioSostenible();
        v.setFechaCreacion(fecha);
        v.setHuellaCarbono(co2);
        return v;
    }

    @Test
    void juntaHistorialYCotizacionesHastaHoy() {
        when(historial.findAllByOrderByFechaAsc()).thenReturn(List.of(dia(HOY.minusDays(10), 2, 30.0), dia(HOY.minusDays(9), 1, 10.0)));
        when(vidrios.findAll()).thenReturn(List.of(
                obra(HOY.minusDays(3).atTime(10, 0), 7.5), obra(HOY.minusDays(3).atTime(15, 0), 2.5),
                obra(null, 99), obra(HOY.plusDays(2).atStartOfDay(), 99)));   // sin fecha y futura: fuera
        var p = new ProyeccionPlataformaService(historial, vidrios).calcular(HOY);
        assertEquals(HOY, p.fin());
        assertEquals(11, p.historia().size());
        assertEquals(30.0, p.historia().get(0));
        assertEquals(10.0, p.historia().get(7));
        assertEquals(0.0, p.historia().get(10));
        assertEquals(List.of("semana"), p.horizontes().stream().map(ProyeccionCo2.Horizonte::clave).toList());
    }

    @Test
    void sinNadaNoProyecta() {
        when(historial.findAllByOrderByFechaAsc()).thenReturn(List.of());
        when(vidrios.findAll()).thenReturn(List.of());
        var p = new ProyeccionPlataformaService(historial, vidrios).calcular(HOY);
        assertTrue(p.historia().isEmpty());
        assertTrue(p.horizontes().isEmpty());
    }
}
