package com.smartglass.service;

import com.smartglass.service.ProyeccionCo2.Banda;
import com.smartglass.service.ProyeccionCo2.Horizonte;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProyeccionCo2Test {

    private static final LocalDate FIN = LocalDate.of(2026, 11, 7);

    private static List<Double> serie(int n, IntToDoubleFunction f) {
        List<Double> s = new ArrayList<>();
        for (int t = 0; t < n; t++) s.add(f.applyAsDouble(t));
        return s;
    }

    private static List<String> claves(ProyeccionCo2.Proyeccion p) {
        return p.horizontes().stream().map(Horizonte::clave).toList();
    }

    @Test
    void recuperaUnaTendenciaLineal() {
        var p = ProyeccionCo2.proyectar(serie(60, t -> 10 + 0.5 * t), FIN);
        assertEquals(10 + 0.5 * 60, p.futuro().get(0).y(), 1e-6);
        assertEquals(List.of("semana", "mes"), claves(p));
        assertEquals(30, p.futuro().size());
        assertEquals(FIN, p.fin());
        assertEquals(60, p.historia().size());
    }

    @Test
    void conMenosDeSieteDiasNoProyecta() {
        var p = ProyeccionCo2.proyectar(serie(6, t -> 5), FIN);
        assertTrue(p.futuro().isEmpty());
        assertTrue(p.horizontes().isEmpty());
        assertTrue(ProyeccionCo2.proyectar(null, FIN).horizontes().isEmpty());
    }

    @Test
    void nuncaBajaDeCero() {
        var p = ProyeccionCo2.proyectar(serie(60, t -> Math.max(0, 100 - 2 * t)), FIN);
        assertFalse(p.futuro().isEmpty());
        p.futuro().forEach(b -> { assertTrue(b.y() >= 0); assertTrue(b.lo() >= 0); });
        p.horizontes().forEach(h -> { assertTrue(h.total() >= 0); assertTrue(h.lo() >= 0); });
    }

    @Test
    void serieEnCerosNoDaNaN() {
        var p = ProyeccionCo2.proyectar(serie(30, t -> 0), FIN);
        assertFalse(p.horizontes().isEmpty());
        p.horizontes().forEach(h -> {
            assertEquals(0, h.total());
            assertEquals(0, h.lo());
            assertEquals(0, h.hi());
        });
    }

    @Test
    void elRangoSeAbreConElTiempo() {
        var p = ProyeccionCo2.proyectar(serie(400, t -> 20 + 0.01 * t + (t % 5) - 2), FIN);
        Banda a = p.futuro().get(0), b = p.futuro().get(299);
        assertTrue(b.hi() - b.lo() > a.hi() - a.lo());
        assertEquals(List.of("semana", "mes", "cinco_meses", "anio"), claves(p));
    }
}
