package com.smartglass.service;

import com.smartglass.model.mysql.Pedido;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FinanzasServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 7);

    private static Pedido pedido(LocalDateTime fecha, double total, String estado, String metodo, boolean compensa) {
        Pedido p = new Pedido();
        p.setFechaPedido(fecha);
        p.setTotal(total);
        p.setEstado(estado);
        p.setMetodoPago(metodo);
        p.setCompensacionCarbono(compensa);
        return p;
    }

    @Test
    void sumaPorMesSinContarCancelados() {
        var r = FinanzasService.calcular(List.of(
                pedido(HOY.atTime(10, 0), 100_000, "PENDIENTE", "PSE", true),
                pedido(HOY.minusMonths(1).atTime(10, 0), 50_000, "ENTREGADO", "TARJETA_CREDITO", false),
                pedido(HOY.minusMonths(2).atTime(10, 0), 25_000, "ENTREGADO", "TARJETA_CREDITO", false),
                pedido(HOY.atTime(11, 0), 999_999, "CANCELADO", "PSE", false),
                pedido(null, 20_000, "ENVIADO", "PSE", false)), HOY);
        assertEquals(195_000, r.ingresos());
        assertEquals(100_000, r.ingresosMes());
        // octubre (cerrado, 50.000) frente a septiembre (25.000); noviembre va a medias y no entra
        assertEquals(100.0, r.variacionMes());
        assertEquals(LocalDate.of(2026, 10, 1), r.mesComparado());
        assertEquals(12, r.meses().size());
        assertEquals("2026-11", r.meses().get(11).clave());
        assertEquals(100_000, r.porCobrar());
        assertEquals(75_000, r.entregado());
        assertEquals(120_000.0, r.porMetodo().get("PSE"));
        assertEquals(1L, r.porEstado().get("CANCELADO"));
        assertEquals(1, r.porAtender());
        assertEquals(25, r.pctCompensados());
        assertEquals(5, r.recientes().size());
    }

    @Test
    void elMesEnCursoSinVentasNoDaUnaCaidaFalsa() {
        // dia 9 sin ventas todavia: antes salia -100 % frente al mes anterior completo
        var r = FinanzasService.calcular(List.of(
                pedido(HOY.minusMonths(1).atTime(10, 0), 80_000, "ENTREGADO", "PSE", false),
                pedido(HOY.minusMonths(2).atTime(10, 0), 80_000, "ENTREGADO", "PSE", false)), HOY);
        assertEquals(0, r.ingresosMes());
        assertEquals(0.0, r.variacionMes());
    }

    @Test
    void sinPedidosNoDivideEntreCero() {
        var r = FinanzasService.calcular(List.of(), HOY);
        assertEquals(0, r.ingresos());
        assertEquals(0, r.ticketPromedio());
        assertNull(r.variacionMes());
    }
}
