package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.HistorialActividad;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.HistorialActividadRepository;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Proyeccion de "Actividad de la plataforma" (/admin/informe-tecnico/proyeccion): CO2 estimado por dia
 * de las cotizaciones guardadas mas el historial de actividad, proyectado con ProyeccionCo2.
 * Los contadores en vivo (ActividadPlataformaService) no usan el historial.
 */
@Service
public class ProyeccionPlataformaService {

    private final HistorialActividadRepository historial;
    private final VidrioSostenibleRepository vidrios;

    public ProyeccionPlataformaService(HistorialActividadRepository historial, VidrioSostenibleRepository vidrios) {
        this.historial = historial;
        this.vidrios = vidrios;
    }

    /* Se pide cada 30 s con la actividad en vivo; la proyeccion cambia poco de un minuto a otro */
    private static final long VIGENCIA_MS = 60_000;
    private volatile ProyeccionCo2.Proyeccion ultima;
    private volatile long ultimaMs;

    public ProyeccionCo2.Proyeccion proyeccion() {
        long ahora = System.currentTimeMillis();
        ProyeccionCo2.Proyeccion p = ultima;
        if (p != null && ahora - ultimaMs < VIGENCIA_MS) return p;
        p = calcular(LocalDate.now(CatalogoProducto.ZONA));
        ultima = p;
        ultimaMs = ahora;
        return p;
    }

    ProyeccionCo2.Proyeccion calcular(LocalDate hoy) {
        TreeMap<LocalDate, Double> porDia = new TreeMap<>();
        for (HistorialActividad d : historial.findAllByOrderByFechaAsc()) {
            if (d.getFecha() != null && !d.getFecha().isAfter(hoy)) porDia.merge(d.getFecha(), d.getCo2Kg(), Double::sum);
        }
        for (VidrioSostenible v : vidrios.findAll()) {
            if (v.getFechaCreacion() == null) continue;
            LocalDate dia = v.getFechaCreacion().toLocalDate();
            if (!dia.isAfter(hoy)) porDia.merge(dia, v.getHuellaCarbono(), Double::sum);
        }
        List<Double> serie = new ArrayList<>();
        if (!porDia.isEmpty()) {
            LocalDate inicio = porDia.firstKey();
            long dias = ChronoUnit.DAYS.between(inicio, hoy);
            for (long i = 0; i <= dias; i++) {
                serie.add(Math.round(porDia.getOrDefault(inicio.plusDays(i), 0.0) * 100) / 100.0);
            }
        }
        return ProyeccionCo2.proyectar(serie, hoy);
    }
}
