package com.smartglass.config;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.HistorialActividad;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.HistorialActividadRepository;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.service.AnalisisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * Historial de actividad anterior a la primera cotizacion guardada, para que la proyeccion de
 * "Actividad de la plataforma" tenga de donde partir desde el primer dia.
 *
 * Solo con DEMO_DATOS=true, una sola vez (guardia propio: la coleccion ya tiene documentos), despues
 * de DemoDatosSeeder (@Order 1). Vive en su propia coleccion: no son cotizaciones, no salen en listas,
 * dashboard ni contadores. El CO2 de cada cotizacion simulada lo calcula el cotizador (AnalisisService).
 * La documentacion tecnica (README) explica de donde sale; la pantalla no lo distingue.
 */
@Component
@ConditionalOnProperty(name = "smartglass.demo.datos", havingValue = "true")
public class HistorialActividadSeeder {

    private static final Logger log = LoggerFactory.getLogger(HistorialActividadSeeder.class);

    static final int DIAS = 730;
    private static final long SEMILLA = 42;
    private static final String[] TIPOS = { "Crudo", "Templado", "Laminado", "TempladoLaminado", "Insulado" };
    private static final int[] ESPESORES = { 4, 6, 8, 10, 12 };

    private final HistorialActividadRepository historial;
    private final VidrioSostenibleRepository vidrios;
    private final AnalisisService analisis;

    public HistorialActividadSeeder(HistorialActividadRepository historial, VidrioSostenibleRepository vidrios,
                                    AnalisisService analisis) {
        this.historial = historial;
        this.vidrios = vidrios;
        this.analisis = analisis;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void cargar() {
        if (historial.count() > 0) return;
        LocalDate hoy = LocalDate.now(CatalogoProducto.ZONA);
        List<LocalDate> fechas = vidrios.findAll().stream()
                .map(VidrioSostenible::getFechaCreacion).filter(Objects::nonNull)
                .map(f -> f.toLocalDate()).filter(f -> !f.isAfter(hoy)).toList();
        LocalDate primerDiaReal = fechas.stream().min(LocalDate::compareTo).orElse(hoy);
        long recientes = fechas.stream().filter(f -> f.isAfter(hoy.minusDays(30))).count();
        double ritmo = Math.max(1, recientes / 30.0);

        Random azar = new Random(SEMILLA);
        List<HistorialActividad> dias = generar(primerDiaReal, ritmo, () -> co2DeUnaCotizacion(azar), azar);
        historial.saveAll(dias);
        log.info("Historial de actividad: {} dias anteriores al {}.", dias.size(), primerDiaReal);
    }

    /**
     * 730 dias que terminan el dia anterior a primerDiaReal. Ritmo con crecimiento (0,35 -> 1),
     * semana laboral (domingo casi vacio) y una ola anual suave; cotizaciones ~ Poisson.
     */
    static List<HistorialActividad> generar(LocalDate primerDiaReal, double ritmoDiario,
                                            DoubleSupplier co2DeUnaCotizacion, Random azar) {
        List<HistorialActividad> dias = new ArrayList<>(DIAS);
        LocalDate inicio = primerDiaReal.minusDays(DIAS);
        for (int i = 0; i < DIAS; i++) {
            LocalDate fecha = inicio.plusDays(i);
            double crecimiento = 0.35 + 0.65 * i / (DIAS - 1.0);
            DayOfWeek d = fecha.getDayOfWeek();
            double semana = d == DayOfWeek.SUNDAY ? 0.25 : d == DayOfWeek.SATURDAY ? 0.75 : 1.1;
            double anual = 1 + 0.15 * Math.sin(2 * Math.PI * fecha.getDayOfYear() / 365.25);
            int cotizaciones = poisson(ritmoDiario * crecimiento * semana * anual, azar);
            double co2 = 0;
            for (int c = 0; c < cotizaciones; c++) co2 += co2DeUnaCotizacion.getAsDouble();
            dias.add(new HistorialActividad(fecha, cotizaciones, Math.round(co2 * 100) / 100.0, "demo"));
        }
        return dias;
    }

    /** Knuth: suficiente para los ritmos de una vidrieria (lambda pequena). */
    private static int poisson(double lambda, Random azar) {
        double limite = Math.exp(-lambda), p = 1;
        int k = 0;
        do { k++; p *= azar.nextDouble(); } while (p > limite);
        return k - 1;
    }

    /** Una configuracion al azar dentro de los limites del formulario, cotizada con el mismo calculo que la web. */
    private double co2DeUnaCotizacion(Random azar) {
        VidrioSostenible v = new VidrioSostenible();
        v.setTipoVidrio(TIPOS[azar.nextInt(TIPOS.length)]);
        v.setAncho(30 + azar.nextInt(221));
        v.setAlto(30 + azar.nextInt(221));
        v.setEspesor(ESPESORES[azar.nextInt(ESPESORES.length)]);
        v.setForma("Rectangular");
        v.setUsarVidrioReciclado(azar.nextDouble() < 0.3);
        return analisis.calcularCotizacionVidrio(v).getHuellaCarbono();
    }
}
