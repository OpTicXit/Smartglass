package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Resistencia con valores de norma y dictamen estructural coherente con la pieza. */
class ResistenciaEstructuralTest {

    private final AnalisisService analisis = new AnalisisService();

    private static VidrioSostenible pieza(String tipo, String uso, double ancho, double alto, double esp, String bordes, int perf) {
        VidrioSostenible v = new VidrioSostenible();
        v.setTipoVidrio(tipo); v.setUsoPrincipal(uso); v.setAncho(ancho); v.setAlto(alto); v.setEspesor(esp);
        v.setAcabadoBordes(bordes); v.setPerforaciones(perf);
        return v;
    }

    private List<String> claves(VidrioSostenible v) {
        return analisis.dictamenEstructural(v).stream().map(AnalisisService.Nota::clave).toList();
    }

    @Test
    void templadoPulidoSinPerforacionesDaElValorDeNorma() {
        assertEquals(120.0, analisis.resistencia(pieza("Templado", "Ventana", 105, 105, 7, "PulidoPlano", 0)).valor());
    }

    @Test
    void recocidoConCorteLimpioYPerforacionesBaja() {
        // 45 x 0,90 (corte limpio) x 0,90 (2 perforaciones en recocido) = 36,45
        assertEquals(36.45, analisis.resistencia(pieza("Crudo", "Mobiliario", 80, 60, 6, "CorteLimpio", 2)).valor(), 0.06);
    }

    @Test
    void laminadoYCapasNoInventanResistencia() {
        assertEquals(analisis.resistencia(pieza("Crudo", "Ventana", 100, 100, 8, "PulidoPlano", 0)).valor(),
                analisis.resistencia(pieza("Laminado", "Ventana", 100, 100, 8, "PulidoPlano", 0)).valor());
    }

    @Test
    void piezaCorrectaNoTieneObservaciones() {
        assertEquals(List.of("dict.espesor_ok", "dict.sin_observaciones"), claves(pieza("Templado", "Ventana", 105, 105, 7, "PulidoPlano", 0)));
    }

    @Test
    void piezaGiganteYDuchaDeRecocidoSeSenalan() {
        List<String> c = claves(pieza("Crudo", "Ducha", 1000, 1000, 8, "CorteLimpio", 6));
        assertTrue(c.containsAll(List.of("dict.requiere_seguridad", "dict.perforaciones_recocido", "dict.excede_lamina")), c.toString());
        assertTrue(!c.contains("dict.sin_observaciones"));
    }

    @Test
    void elCalculoDeLaCotizacionGuardaLaMismaResistencia() {
        VidrioSostenible v = pieza("Templado", "Ventana", 105, 105, 7, "CorteLimpio", 0);
        assertEquals(analisis.resistencia(v).valor(), analisis.calcularCotizacionVidrio(v).getResistenciaEstimada());
    }
}
