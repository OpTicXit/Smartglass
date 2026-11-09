package com.smartglass.service;

import com.smartglass.dto.CotizacionVidrioRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Cuestionario opcional de uso: solo codigos conocidos y un comentario corto y limpio. */
class CuestionarioUsoTest {

    @Test
    void lasRespuestasCerradasSoloAceptanCodigosConocidos() {
        assertEquals("FACHADA", VidrioSostenibleService.opcion("FACHADA", VidrioSostenibleService.EXPOSICION));
        assertEquals("LEED", VidrioSostenibleService.opcion(" LEED ", VidrioSostenibleService.CERTIFICACION));
        assertNull(VidrioSostenibleService.opcion("", VidrioSostenibleService.EXPOSICION), "omitida");
        assertNull(VidrioSostenibleService.opcion(null, VidrioSostenibleService.IMPACTO));
        assertNull(VidrioSostenibleService.opcion("<script>", VidrioSostenibleService.ACUSTICO), "valor manipulado");
    }

    @Test
    void elComentarioSeLimpiaYTieneTope() {
        assertNull(VidrioSostenibleService.comentario("   "));
        assertEquals("Ventana al mar con mucho viento", VidrioSostenibleService.comentario("  Ventana al mar\n\tcon   mucho viento "));
        assertEquals(300, VidrioSostenibleService.comentario("a".repeat(310)).length());

        CotizacionVidrioRequest r = new CotizacionVidrioRequest();
        r.setAncho(100); r.setAlto(100); r.setEspesor(6);
        r.setUsoComentario("x".repeat(400));
        assertThrows(IllegalArgumentException.class, () -> VidrioSostenibleService.validar(r), "un comentario enorme se rechaza");
    }
}
