package com.smartglass.service;

import com.smartglass.controller.AdminController;
import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.dto.PuntoCorte;
import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mongo.VidrioSostenible;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void lasMedidasGigantesSeRechazanYElTopeExactoPasa() {
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(1e25, 105, 7)));
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(1000.5, 105, 7)));
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(cotizacion(105, 105, 51)));
        assertDoesNotThrow(() -> VidrioSostenibleService.validar(cotizacion(1000, 1000, 50)));
    }

    @Test
    void unCorteNoPuedeSaltarseElTopeDeLasMedidas() {
        VidrioSostenible gigante = new VidrioSostenible();
        gigante.setForma("Personalizada");
        gigante.setTipoVidrio("Crudo");
        gigante.setAncho(105);
        gigante.setAlto(105);
        gigante.setEspesor(7);
        gigante.setPuntosPersonalizados(java.util.List.of(new PuntoCorte(0, 0), new PuntoCorte(5000, 0), new PuntoCorte(5000, 5000)));
        assertFalse(new AnalisisService().calcularCotizacionVidrio(gigante).isCorteAplicado());
    }

    @Test
    void unProductoDelAdminNoAceptaValoresImposibles() {
        assertFalse(AdminController.productoEnLimites(producto(Double.NaN, null)));
        assertFalse(AdminController.productoEnLimites(producto(Double.POSITIVE_INFINITY, null)));
        assertFalse(AdminController.productoEnLimites(producto(-1, null)));
        assertFalse(AdminController.productoEnLimites(producto(1e25, null)));
        assertFalse(AdminController.productoEnLimites(producto(50000, 1e25)));
        assertTrue(AdminController.productoEnLimites(producto(50000, null)));
        assertTrue(AdminController.productoEnLimites(producto(50000, 120.5)));
    }

    private static CatalogoProducto producto(double precio, Double ancho) {
        CatalogoProducto p = new CatalogoProducto();
        p.setPrecio(precio);
        p.setAncho(ancho);
        return p;
    }

    @Test
    void elDisenoSoloAceptaUnPngDeVerdad() {
        String png = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(
                new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0 });
        assertEquals(true, VidrioSostenibleService.disenoValido(null));
        assertEquals(true, VidrioSostenibleService.disenoValido(png));
        assertEquals(false, VidrioSostenibleService.disenoValido("data:image/svg+xml;base64,PHN2Zz4="));
        assertEquals(false, VidrioSostenibleService.disenoValido("javascript:alert(1)"));
        assertEquals(false, VidrioSostenibleService.disenoValido("data:image/png;base64,no-es-base64!"));
        assertEquals(false, VidrioSostenibleService.disenoValido("data:image/png;base64,QUJDREVGR0hJSktM"));
        assertEquals(false, VidrioSostenibleService.disenoValido("data:image/png;base64," + "A".repeat(700_000)));
    }

    private static CotizacionVidrioRequest cotizacion(double ancho, double alto, double espesor) {
        CotizacionVidrioRequest r = new CotizacionVidrioRequest();
        r.setAncho(ancho);
        r.setAlto(alto);
        r.setEspesor(espesor);
        return r;
    }
}
