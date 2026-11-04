package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.repository.mysql.PedidoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActividadPlataformaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 10, 0);

    private static VidrioSostenible obra(LocalDateTime fecha, double co2, boolean reciclado, String estado) {
        VidrioSostenible v = new VidrioSostenible();
        v.setFechaCreacion(fecha);
        v.setHuellaCarbono(co2);
        v.setUsarVidrioReciclado(reciclado);
        v.setEstado(estado);
        return v;
    }

    private static Pedido pedido(LocalDateTime fecha) {
        Pedido p = new Pedido();
        p.setFechaPedido(fecha);
        return p;
    }

    @Test
    void sumaPorPeriodoSinPerderLoAntiguoNiLoSinFecha() {
        VidrioSostenibleRepository vidrios = mock(VidrioSostenibleRepository.class);
        PedidoRepository pedidos = mock(PedidoRepository.class);
        when(vidrios.findAll()).thenReturn(List.of(
                obra(AHORA.minusHours(1), 10, true, "COTIZACION"),
                obra(AHORA.minusDays(6), 20, false, "APROBADO"),
                obra(AHORA.minusDays(7), 30, false, "COTIZACION"),   // fuera de los 7 dias, dentro de los 30
                obra(AHORA.minusDays(40), 40, true, "RECHAZADA"),    // fuera de la grafica, cuenta en el total
                obra(null, 5, false, "APROBADO")));
        when(pedidos.findAll()).thenReturn(List.of(pedido(AHORA.minusDays(2)), pedido(AHORA.minusDays(20)), pedido(null)));

        ActividadPlataformaService.Actividad a = new ActividadPlataformaService(vidrios, pedidos).calcular(AHORA);

        assertEquals(5, a.cotizaciones());
        assertEquals(2, a.pendientes());
        assertEquals(105.0, a.co2Kg());
        assertEquals(2, a.cotizacionesSemana());
        assertEquals(30.0, a.co2KgSemana());
        assertEquals(3, a.pedidos());
        assertEquals(1, a.pedidosSemana());
        assertEquals(40, a.pctReciclado());
        assertEquals(30, a.dias().size());
        assertEquals(LocalDate.of(2026, 11, 4), a.dias().get(29).fecha());
        assertEquals(10.0, a.dias().get(29).co2Kg());
        assertEquals(30.0, a.dias().get(22).co2Kg());
        assertEquals(1, a.dias().get(22).cotizaciones());
    }

    @Test
    void sinDatosDevuelveCeros() {
        VidrioSostenibleRepository vidrios = mock(VidrioSostenibleRepository.class);
        PedidoRepository pedidos = mock(PedidoRepository.class);
        when(vidrios.findAll()).thenReturn(List.of());
        when(pedidos.findAll()).thenReturn(List.of());

        ActividadPlataformaService.Actividad a = new ActividadPlataformaService(vidrios, pedidos).calcular(AHORA);

        assertEquals(0, a.pctReciclado());
        assertEquals(0.0, a.co2Kg());
        assertEquals(30, a.dias().size());
    }
}
