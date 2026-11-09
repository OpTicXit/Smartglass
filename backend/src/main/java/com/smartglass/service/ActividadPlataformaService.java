package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.repository.mysql.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Actividad en vivo del informe tecnico (/admin/informe-tecnico/vivo): suma lo guardado en la plataforma.
 * Cache de 15 s: el panel la consulta cada 30 s por pestana y asi varias pestanas no repiten el calculo.
 * El CO2 es el del cotizador (AnalisisService), no el del modelo predictivo.
 */
@Service
public class ActividadPlataformaService {

    private static final int DIAS_GRAFICA = 90;   // la pagina muestra 7, 30 o 90
    private static final int DIAS_SEMANA = 7;

    private final VidrioSostenibleRepository vidrioRepository;
    private final PedidoRepository pedidoRepository;

    public ActividadPlataformaService(VidrioSostenibleRepository vidrioRepository, PedidoRepository pedidoRepository) {
        this.vidrioRepository = vidrioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public record Dia(LocalDate fecha, int cotizaciones, double co2Kg) {}

    public record Actividad(LocalDateTime generado, int cotizaciones, int pendientes, int pedidos, int pedidosSemana,
                            double co2Kg, int cotizacionesSemana, double co2KgSemana, int pctReciclado, List<Dia> dias) {}

    /* Cada pestana abierta del informe pide esto cada 30 s y el calculo lee todas las cotizaciones y pedidos:
       se reutiliza el resultado unos segundos. shortcut: lee todo con findAll; con miles de registros,
       pasar los totales a una agregacion de Mongo y la grafica a un rango de fechas. */
    private static final long VIGENCIA_MS = 15_000;
    private volatile Actividad ultima;
    private volatile long ultimaMs;

    public Actividad actividad() {
        long ahora = System.currentTimeMillis();
        Actividad a = ultima;
        if (a != null && ahora - ultimaMs < VIGENCIA_MS) return a;
        a = calcular(LocalDateTime.now());
        ultima = a;
        ultimaMs = ahora;
        return a;
    }

    Actividad calcular(LocalDateTime ahora) {
        List<VidrioSostenible> obras = vidrioRepository.findAll();
        List<Pedido> pedidos = pedidoRepository.findAll();
        LocalDate hoy = ahora.toLocalDate();
        LocalDate inicioSemana = hoy.minusDays(DIAS_SEMANA - 1L);

        Map<LocalDate, int[]> cuenta = new LinkedHashMap<>();
        Map<LocalDate, double[]> co2 = new LinkedHashMap<>();
        for (int i = DIAS_GRAFICA - 1; i >= 0; i--) {
            cuenta.put(hoy.minusDays(i), new int[1]);
            co2.put(hoy.minusDays(i), new double[1]);
        }

        int pendientes = 0, conReciclado = 0, semana = 0;
        double total = 0, totalSemana = 0;
        for (VidrioSostenible v : obras) {
            total += v.getHuellaCarbono();
            if ("COTIZACION".equals(v.getEstado())) pendientes++;
            if (v.isUsarVidrioReciclado()) conReciclado++;
            if (v.getFechaCreacion() == null) continue;
            LocalDate dia = v.getFechaCreacion().toLocalDate();
            if (!dia.isBefore(inicioSemana) && !dia.isAfter(hoy)) {
                semana++;
                totalSemana += v.getHuellaCarbono();
            }
            if (cuenta.containsKey(dia)) {
                cuenta.get(dia)[0]++;
                co2.get(dia)[0] += v.getHuellaCarbono();
            }
        }

        int pedidosSemana = (int) pedidos.stream().map(Pedido::getFechaPedido)
                .filter(f -> f != null && !f.toLocalDate().isBefore(inicioSemana) && !f.toLocalDate().isAfter(hoy))
                .count();

        List<Dia> dias = new ArrayList<>();
        cuenta.forEach((dia, n) -> dias.add(new Dia(dia, n[0], redondear(co2.get(dia)[0]))));
        int pct = obras.isEmpty() ? 0 : (int) Math.round(conReciclado * 100.0 / obras.size());
        return new Actividad(ahora, obras.size(), pendientes, pedidos.size(), pedidosSemana,
                redondear(total), semana, redondear(totalSemana), pct, dias);
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
