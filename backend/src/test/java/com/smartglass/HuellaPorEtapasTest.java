package com.smartglass;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.service.AnalisisService;
import com.smartglass.service.ResultadoAnalisisVidrio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * U4 · Anclas A-D del modelo de huella por etapas. glass-preview.js (calcular) debe dar lo mismo:
 * si cambia una constante en un lado y no en el otro, este test o las anclas del configurador fallan.
 */
class HuellaPorEtapasTest {

    private final AnalisisService analisis = new AnalisisService();

    private ResultadoAnalisisVidrio pieza(String tipo, double ancho, double alto, double espesor, boolean eco, boolean renovable) {
        VidrioSostenible v = new VidrioSostenible();
        v.setTipoVidrio(tipo);
        v.setAncho(ancho);
        v.setAlto(alto);
        v.setEspesor(espesor);
        v.setForma("Rectangular");
        v.setUsoPrincipal("Mobiliario");
        v.setCantidadCapas(1);
        v.setUsarVidrioReciclado(eco);
        v.setEnergiaRenovable(renovable);
        return analisis.calcularCotizacionVidrio(v);
    }

    /** Area exacta, como el configurador y VidrioSostenible.getIntensidadCarbono (areaTotal va redondeada). */
    private static double intensidad(ResultadoAnalisisVidrio r, double ancho, double alto) {
        return Math.round(r.getHuellaCarbono() / (ancho * alto / 10000.0) * 10.0) / 10.0;
    }

    @Test
    void anclaA_templado7mmConCalcinYRenovable() {
        ResultadoAnalisisVidrio r = pieza("Templado", 105, 105, 7, true, true);
        assertEquals(19.29, r.getPesoEstimado(), 0.01);
        assertEquals(19.13, r.getHuellaCarbono(), 0.01);
        assertEquals(17.4, intensidad(r, 105, 105), 0.001);   // Moderado
        assertEquals(18.38, r.getHuellaEtapas().get("fusion"), 0.01);
        assertEquals(0.19, r.getHuellaEtapas().get("templado"), 0.01);
        assertEquals(0.56, r.getHuellaEtapas().get("transporte"), 0.01);
    }

    @Test
    void anclaB_templadoLaminado20mmSinOpciones() {
        ResultadoAnalisisVidrio r = pieza("TempladoLaminado", 105, 105, 20, false, false);
        assertEquals(68.57, r.getHuellaCarbono(), 0.01);
        assertEquals(62.2, intensidad(r, 105, 105), 0.001);   // Alto
        assertEquals(3.97, r.getHuellaEtapas().get("laminado"), 0.01);
        assertEquals(3.88, r.getHuellaEtapas().get("templado"), 0.01);
    }

    @Test
    void anclaC_templado40mm() {
        assertEquals(99.1, intensidad(pieza("Templado", 105, 105, 40, true, true), 105, 105), 0.001);   // Alto
    }

    @Test
    void anclaD_templado400x400() {
        ResultadoAnalisisVidrio r = pieza("Templado", 400, 400, 8, true, true);
        assertEquals(320.0, r.getPesoEstimado(), 0.01);
        assertEquals(317.26, r.getHuellaCarbono(), 0.01);
        assertEquals(19.8, intensidad(r, 400, 400), 0.001);   // Moderado
    }

    @Test
    void capaActivaSumaOchentaPorCientoDeLaFusion() {
        ResultadoAnalisisVidrio r = pieza("Fotovoltaico", 100, 100, 6, false, false);
        assertEquals(r.getHuellaEtapas().get("fusion") * 0.8, r.getHuellaEtapas().get("capaActiva"), 0.02);
    }

    @Test
    void catalogoUsaLasMismasEtapas() {
        // 100 x 100 x 6 templado, sin opciones: fusion 15,45 + templado 1,056 + transporte 0,4365 = 16,94
        assertEquals(16.94, analisis.calcularHuellaCarbonoItem(100.0, 100.0, 6.0, "Templado", 1), 0.01);
    }
}
