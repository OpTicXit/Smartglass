package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import com.smartglass.repository.mysql.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * T4 · Cifras reales de la plataforma para las paginas publicas (acerca e inicio).
 *
 * - Solo suma datos ya guardados (cotizaciones y pedidos): no recalcula CO2 ni precio.
 * - Cache de 5 minutos para no recorrer las colecciones en cada visita.
 * - Si la base no responde, devuelve ceros y la vista oculta el bloque (nunca rompe la pagina).
 */
@Service
public class EstadisticasPlataformaService {

    private static final long VIGENCIA_MS = 5 * 60 * 1000;

    private final VidrioSostenibleRepository vidrioRepository;
    private final PedidoRepository pedidoRepository;

    private volatile Cifras cache;
    private volatile long calculadoEn;

    public EstadisticasPlataformaService(VidrioSostenibleRepository vidrioRepository, PedidoRepository pedidoRepository) {
        this.vidrioRepository = vidrioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    /** Cifras inmutables para la vista (getters clasicos para SpEL). */
    public static final class Cifras {
        private final long obras, pedidos, sellosOro;
        private final double kgCalcin, kgRetoma;
        private final int pctReciclado;

        Cifras(long obras, long pedidos, long sellosOro, double kgCalcin, double kgRetoma, int pctReciclado) {
            this.obras = obras; this.pedidos = pedidos; this.sellosOro = sellosOro;
            this.kgCalcin = kgCalcin; this.kgRetoma = kgRetoma; this.pctReciclado = pctReciclado;
        }
        public long getObras() { return obras; }
        public long getPedidos() { return pedidos; }
        public long getSellosOro() { return sellosOro; }
        /** kg de vidrio reciclado (calcin) incorporado en las obras cotizadas. */
        public double getKgCalcin() { return kgCalcin; }
        /** kg de vidrio que los clientes pidieron retirar para reciclar (retoma, T3). */
        public double getKgRetoma() { return kgRetoma; }
        /** % de obras que usan material reciclado. */
        public int getPctReciclado() { return pctReciclado; }
        public boolean isHayDatos() { return obras > 0; }
    }

    public Cifras cifras() {
        long ahora = System.currentTimeMillis();
        Cifras c = cache;
        if (c != null && ahora - calculadoEn < VIGENCIA_MS) return c;
        try {
            List<VidrioSostenible> obras = vidrioRepository.findAll();
            long n = obras.size();
            long conReciclado = obras.stream().filter(VidrioSostenible::isUsarVidrioReciclado).count();
            c = new Cifras(
                    n,
                    pedidoRepository.count(),
                    obras.stream().filter(v -> "ORO".equals(v.getCompromisoNivel())).count(),
                    obras.stream().mapToDouble(VidrioSostenible::getVidrioRecicladoKg).sum(),
                    obras.stream().filter(VidrioSostenible::isRetomaVidrio).mapToDouble(VidrioSostenible::getPesoEstimado).sum(),
                    n > 0 ? (int) Math.round(conReciclado * 100.0 / n) : 0);
        } catch (RuntimeException e) {
            c = new Cifras(0, 0, 0, 0, 0, 0);
        }
        cache = c;
        calculadoEn = ahora;
        return c;
    }
}
