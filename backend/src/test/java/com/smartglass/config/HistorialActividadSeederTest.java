package com.smartglass.config;

import com.smartglass.model.mongo.HistorialActividad;
import com.smartglass.repository.mongo.HistorialActividadRepository;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.service.AnalisisService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistorialActividadSeederTest {

    private static final LocalDate D = LocalDate.of(2026, 9, 1);

    private static List<HistorialActividad> generar(long semilla) {
        return HistorialActividadSeeder.generar(D, 3.0, () -> 20.0, new Random(semilla));
    }

    @Test
    void generaDosAniosQueTerminanAntesDeLoReal() {
        var h = generar(42);
        assertEquals(730, h.size());
        assertEquals(D.minusDays(1), h.get(729).getFecha());
        assertEquals(D.minusDays(730), h.get(0).getFecha());
    }

    @Test
    void elFinalEmpalmaConElRitmoReal() {
        var h = generar(42);
        double media = h.subList(700, 730).stream().mapToInt(HistorialActividad::getCotizaciones).average().orElse(0);
        assertEquals(3.0, media, 3.0 * 0.35);
        assertEquals(h.get(729).getCotizaciones() * 20.0, h.get(729).getCo2Kg(), 1e-9);
    }

    @Test
    void esReproducible() {
        assertEquals(generar(42).stream().map(HistorialActividad::getCotizaciones).toList(),
                     generar(42).stream().map(HistorialActividad::getCotizaciones).toList());
    }

    @Test
    void noSeRepiteNiTocaCotizaciones() {
        HistorialActividadRepository historial = mock(HistorialActividadRepository.class);
        VidrioSostenibleRepository vidrios = mock(VidrioSostenibleRepository.class);
        when(historial.count()).thenReturn(5L);
        new HistorialActividadSeeder(historial, vidrios, mock(AnalisisService.class)).cargar();
        verify(historial, never()).saveAll(any());
        verify(vidrios, never()).save(any());
        verify(vidrios, never()).findAll();
    }
}
