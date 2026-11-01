package com.smartglass.service;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.ItemCarrito;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Entradas manipuladas que antes daban pedidos o cotizaciones con valores negativos. */
class LimitesEntradaTest {

    @Test
    void laCantidadDelCarritoNoDesbordaANegativo() {
        ItemCarrito item = new ItemCarrito(null, Integer.MAX_VALUE);
        assertEquals(ItemCarrito.MAX_CANTIDAD, item.getCantidad());
        item.incrementarCantidad(Integer.MAX_VALUE);
        assertEquals(ItemCarrito.MAX_CANTIDAD, item.getCantidad());
    }

    @Test
    void unaCotizacionNormalPasa() {
        assertDoesNotThrow(() -> VidrioSostenibleService.validar(cotizacion(105, 105, 7)));
    }

    @Test
    void medidasNegativasNaNOInfinitasSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(-300, 105, 7)));
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(105, 105, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(Double.POSITIVE_INFINITY, 105, 7)));
        CotizacionVidrioRequest perforada = cotizacion(105, 105, 7);
        perforada.setPerforaciones(-1000);
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(perforada));
    }

    private static CotizacionVidrioRequest cotizacion(double ancho, double alto, double espesor) {
        CotizacionVidrioRequest r = new CotizacionVidrioRequest();
        r.setAncho(ancho);
        r.setAlto(alto);
        r.setEspesor(espesor);
        return r;
    }
}
