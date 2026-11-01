package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;

import java.util.Map;
import java.util.Optional;

/**
 * T2 · D3 balanceo de precios del CATALOGO (datos de negocio en Mongo). No toca
 * AnalisisService ni el precio de la cotizacion personalizada (contrato).
 *
 * Vidrio plano: precio = area (m2) x (base del tipo + tarifa por mm x espesor) x acabado x escala,
 *   con escala = area^-0,2 por encima de 1 m2 (los formatos grandes bajan su precio por m2).
 *   La tabla se calibro con las piezas que ya eran coherentes (Templado 8 mm = $180.000/m2,
 *   Flotado 6 mm = $90.000/m2, Laminado 8 mm = $200.000/m2, Inteligente 10 mm = $950.000/m2...)
 *   y corrige las que no escalaban con su medida (p. ej. una mampara de 2 m2 mas barata por m2
 *   que la lamina de 1 m2 del mismo vidrio, o un espejo solar de 4 m2 a $300.000/m2).
 * Esculturas: precio = base del tamano + tarifa por litro de volumen envolvente; una pieza de
 *   mesa ya no cuesta lo mismo que una lamina de 1 m2.
 * Redondeo: $1.000 en vidrio plano, $10.000 en esculturas.
 * Regla de aplicacion (decision del usuario, v3): el balanceo SOLO BAJA precios. Se cobra
 * el menor entre el precio original y el sugerido; nada sube por el esquema.
 */
public final class EsquemaPreciosCatalogo {

    public static final String VERSION = "T2-v3";

    /** true si el producto se balanceo con una version anterior del esquema (se recalcula). */
    public static boolean esVersionAnterior(String esquema) {
        return esquema != null && esquema.startsWith("T2-") && !VERSION.equals(esquema);
    }

    private EsquemaPreciosCatalogo() {
    }

    /** tipo -> {base por m2, tarifa por mm de espesor por m2} (COP). */
    private static final Map<String, double[]> PLANO = Map.ofEntries(
            Map.entry("crudo", new double[]{30_000, 10_000}),
            Map.entry("espejo", new double[]{45_000, 12_500}),
            Map.entry("decorativo", new double[]{50_000, 12_000}),
            Map.entry("templado", new double[]{60_000, 15_000}),
            Map.entry("laminado", new double[]{80_000, 15_000}),
            Map.entry("control solar", new double[]{70_000, 15_000}),
            Map.entry("solar", new double[]{70_000, 15_000}),
            Map.entry("tratado", new double[]{100_000, 20_000}),
            Map.entry("curvo", new double[]{120_000, 18_000}),
            Map.entry("inteligente", new double[]{850_000, 10_000})
    );
    /** El DVH se cobra por m2 de unidad armada (dos hojas + camara), no por mm de espesor total. */
    private static final double INSULADO_POR_M2 = 190_000;

    /** Acabado artesanal sobre la base del tipo (decorativos), por codigo de producto. */
    private static final Map<String, Double> ACABADO = Map.of(
            "VID-008", 2.0,    // vitral decorativo: emplomado a mano
            "VID-011", 1.15,   // texturizado de colores
            "VID-016", 1.4     // lacado opaco: pintura horneada
    );

    /** tamano -> {base, tarifa por litro} (COP). */
    private static final Map<String, double[]> ESCULTURA = Map.of(
            "pequena", new double[]{40_000, 8_000},
            "mediana", new double[]{350_000, 10_000},
            "grande", new double[]{1_800_000, 6_000}
    );

    /** Precio sugerido por el esquema; vacio si al producto le faltan datos para calcularlo. */
    public static Optional<Double> precioSugerido(CatalogoProducto p) {
        if (p.getAncho() == null || p.getAlto() == null || p.getEspesor() == null
                || p.getAncho() <= 0 || p.getAlto() <= 0 || p.getEspesor() <= 0) return Optional.empty();

        if (p.isEscultura()) {
            double[] t = p.getTamano() == null ? null : ESCULTURA.get(p.getTamano());
            if (t == null) return Optional.empty();
            double litros = p.getAncho() * p.getAlto() * p.getEspesor() / 1000.0;   // cm3 -> L
            return Optional.of(redondear(t[0] + t[1] * litros, 10_000));
        }

        String tipo = p.getTipo() == null ? "" : p.getTipo().trim().toLowerCase();
        double area = p.getAncho() * p.getAlto() / 10_000.0;                         // cm2 -> m2
        double porM2;
        if ("insulado".equals(tipo)) {
            porM2 = INSULADO_POR_M2;
        } else {
            double[] t = PLANO.get(tipo);
            if (t == null) return Optional.empty();
            porM2 = t[0] + t[1] * p.getEspesor();
        }
        double acabado = p.getCodigo() == null ? 1.0 : ACABADO.getOrDefault(p.getCodigo(), 1.0);
        double escala = area > 1.0 ? Math.pow(area, -0.2) : 1.0;
        return Optional.of(redondear(area * porM2 * acabado * escala, 1_000));
    }

    /** Precio que se aplica: el sugerido solo si abarata el original (nunca sube). */
    public static Optional<Double> precioBalanceado(CatalogoProducto p, double original) {
        return precioSugerido(p).map(sugerido -> Math.min(original, sugerido));
    }

    private static double redondear(double valor, double paso) {
        return Math.round(valor / paso) * paso;
    }
}
