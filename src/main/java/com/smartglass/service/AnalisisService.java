package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import org.springframework.stereotype.Service;

/**
 * Aisla TODA la logica matematica de eficiencia energetica y huella
 * de CO2 de SmartGlass, para que ni los controllers ni los servicios
 * de persistencia (PedidoService, VidrioSostenibleService) tengan
 * que conocer formulas ni constantes de proceso.
 *
 * NOTA / SUPUESTO IMPORTANTE: no se me proporciono el algoritmo de
 * negocio original para cotizar un VidrioSostenible (el modelo ya
 * traia campos como cantidadSilice, energiaRequerida, etc. pero sin
 * un servicio que los calculara). Las formulas de composicion,
 * energia y precio de este archivo son una base razonable basada en
 * vidrio flotado tipo soda-lime, pensada para que ajustes los
 * coeficientes (SODA_LIME_*, ENERGIA_POR_KG, PRECIO_POR_KG, etc.) a
 * los valores reales de tu proceso productivo. La logica de huella
 * de carbono por producto (migrada desde PedidoService) si replica
 * exactamente el calculo que ya tenias.
 */
@Service
public class AnalisisService {

    // --- Constantes fisicas compartidas ---
    private static final double DENSIDAD_VIDRIO = 2.5; // kg / (m2 * mm)
    private static final double CONVERSION_M2 = 10000.0; // cm2 -> m2

    // --- Factores de emision de CO2 por proceso (kg CO2 / kg vidrio) ---
    private static final double FACTOR_TEMPLADO = 0.85;
    private static final double FACTOR_LAMINADO = 1.2;
    private static final double FACTOR_INSULADO = 1.5;
    private static final double FACTOR_DEFAULT = 1.0;

    // --- Composicion tipica de vidrio soda-lime (referencia industria) ---
    private static final double PORC_SILICE = 0.72;
    private static final double PORC_SOSA = 0.13;
    private static final double PORC_CALIZA = 0.09;
    private static final double PORC_ALUMINA = 0.04;

    // --- Energia y costo base ---
    private static final double ENERGIA_POR_KG_KWH = 2.5; // kWh requeridos por kg de vidrio fundido
    private static final double PRECIO_BASE_POR_KG = 4500.0; // COP por kg
    private static final double RESISTENCIA_BASE_MPA = 45.0; // vidrio recocido estandar

    /**
     * Calcula la huella de carbono de un item de pedido, a partir de
     * sus dimensiones y tipo de proceso. Migrado tal cual desde
     * PedidoService para que el calculo viva en un unico lugar.
     */
    public double calcularHuellaCarbonoItem(Double ancho, Double alto, Double espesor, String tipo, int cantidad) {
        double anchoVal = (ancho != null) ? ancho : 0;
        double altoVal = (alto != null) ? alto : 0;
        double espesorVal = (espesor != null) ? espesor : 0;

        double area = (anchoVal * altoVal) / CONVERSION_M2;
        double peso = area * espesorVal * DENSIDAD_VIDRIO;

        double co2 = peso * obtenerFactorEmision(tipo) * cantidad;
        return Math.round(co2 * 100.0) / 100.0;
    }

    private double obtenerFactorEmision(String tipo) {
        if (tipo == null) return FACTOR_DEFAULT;
        return switch (tipo.toLowerCase().trim()) {
            case "templado" -> FACTOR_TEMPLADO;
            case "laminado" -> FACTOR_LAMINADO;
            case "insulado" -> FACTOR_INSULADO;
            default -> FACTOR_DEFAULT;
        };
    }

    /**
     * Calcula toda la cotizacion tecnica/ambiental de un vidrio
     * sostenible a partir de sus dimensiones y opciones de
     * fabricacion. Devuelve un objeto de transporte interno que
     * VidrioSostenibleService usa para poblar la entidad antes de
     * guardarla.
     */
    public ResultadoAnalisisVidrio calcularCotizacionVidrio(VidrioSostenible datos) {
        ResultadoAnalisisVidrio resultado = new ResultadoAnalisisVidrio();

        double area = (datos.getAncho() * datos.getAlto()) / CONVERSION_M2;
        double peso = area * datos.getEspesor() * DENSIDAD_VIDRIO * Math.max(1, datos.getCantidadCapas());

        resultado.setAreaTotal(redondear(area));
        resultado.setPesoEstimado(redondear(peso));

        // Composicion de materiales (se reduce proporcionalmente si se usa vidrio reciclado)
        double proporcionMaterialNuevo = 1 - (datos.isUsarVidrioReciclado()
                ? datos.getPorcentajeReciclado() / 100.0
                : 0.0);

        resultado.setCantidadSilice(redondear(peso * PORC_SILICE * proporcionMaterialNuevo));
        resultado.setCantidadSosa(redondear(peso * PORC_SOSA * proporcionMaterialNuevo));
        resultado.setCantidadCaliza(redondear(peso * PORC_CALIZA * proporcionMaterialNuevo));
        resultado.setCantidadAlumina(redondear(peso * PORC_ALUMINA * proporcionMaterialNuevo));

        // Energia requerida: el vidrio reciclado reduce el punto de fusion necesario
        double energia = peso * ENERGIA_POR_KG_KWH * proporcionMaterialNuevo
                + peso * ENERGIA_POR_KG_KWH * 0.6 * (1 - proporcionMaterialNuevo);
        if (datos.isEnergiaRenovable()) {
            energia *= 0.5; // la energia renovable no reduce el consumo, pero si su impacto en CO2 (ver abajo)
        }
        resultado.setEnergiaRequerida(redondear(energia));

        // Huella de carbono: depende del proceso, del material reciclado y de si la energia es renovable
        double co2 = peso * obtenerFactorEmision(datos.getTratamiento());
        co2 *= proporcionMaterialNuevo + (1 - proporcionMaterialNuevo) * 0.3; // reciclar reduce fuertemente el CO2
        if (datos.isEnergiaRenovable()) {
            co2 *= 0.4;
        }
        if (datos.isCompensarCarbono()) {
            co2 = 0.0; // se asume compensacion total via creditos de carbono
        }
        resultado.setHuellaCarbono(redondear(co2));

        // Precio estimado: base por kg + recargos por tratamiento/capas/corte especial
        double precio = peso * PRECIO_BASE_POR_KG;
        precio *= factorTratamiento(datos.getTratamiento());
        precio *= datos.getCantidadCapas() > 1 ? 1 + (0.15 * (datos.getCantidadCapas() - 1)) : 1;
        if (datos.isRequiereCorteEspecial()) {
            precio *= 1.2;
        }
        resultado.setPrecioEstimado(redondear(precio));

        // Resistencia estimada: base + bonus por tratamiento y capas
        double resistencia = RESISTENCIA_BASE_MPA * factorResistenciaTratamiento(datos.getTratamiento());
        resistencia += (datos.getCantidadCapas() - 1) * 10;
        resultado.setResistenciaEstimada(redondear(resistencia));

        resultado.setRecomendacion(generarRecomendacion(datos, resultado));

        return resultado;
    }

    private double factorTratamiento(String tratamiento) {
        if (tratamiento == null) return 1.0;
        return switch (tratamiento.toLowerCase().trim()) {
            case "templado" -> 1.3;
            case "laminado" -> 1.5;
            case "insulado" -> 1.8;
            default -> 1.0;
        };
    }

    private double factorResistenciaTratamiento(String tratamiento) {
        if (tratamiento == null) return 1.0;
        return switch (tratamiento.toLowerCase().trim()) {
            case "templado" -> 4.0; // el templado multiplica varias veces la resistencia mecanica
            case "laminado" -> 1.5;
            case "insulado" -> 1.8;
            default -> 1.0;
        };
    }

    private String generarRecomendacion(VidrioSostenible datos, ResultadoAnalisisVidrio resultado) {
        if (!datos.isUsarVidrioReciclado() && resultado.getHuellaCarbono() > 50) {
            return "Considera usar vidrio reciclado para reducir significativamente la huella de CO2.";
        }
        if (!datos.isEnergiaRenovable()) {
            return "Optar por energia renovable en la fabricacion reduciria aun mas el impacto ambiental.";
        }
        return "Esta configuracion ya tiene un perfil ambiental favorable.";
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
