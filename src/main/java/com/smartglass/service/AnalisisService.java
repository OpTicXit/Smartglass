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

    // --- Precio base por m2 segun tipo de vidrio (portado 1:1 del JS de vidrio-personalizado-form.html) ---
    private static final double PRECIO_M2_DEFAULT = 80000;    // Crudo / no reconocido
    private static final double PRECIO_M2_TEMPLADO = 120000;
    private static final double PRECIO_M2_LAMINADO = 160000;
    private static final double PRECIO_M2_TEMPLADO_LAMINADO = 280000;
    private static final double PRECIO_M2_INSULADO = 220000;
    private static final double PRECIO_M2_INTELIGENTE = 850000;
    private static final double PRECIO_M2_FOTOVOLTAICO = 1200000;
    private static final double ESPESOR_BASE_NORMALIZACION_MM = 5.0;

    private static final double RECARGO_CONTROL_SOLAR_ANTIREFLEJO = 50000;
    private static final double RECARGO_SERIGRAFIADO = 80000;
    private static final double RECARGO_TINTADO = 25000;

    private static final double COSTO_PULIDO_POR_METRO_LINEAL = 8000;
    private static final double COSTO_BISELADO_POR_METRO_LINEAL = 15000;
    private static final double COSTO_POR_PERFORACION = 12000;
    private static final double RECARGO_COMPENSACION_CARBONO = 5000;
    private static final double DESCUENTO_RECICLADO = 0.95; // 5% de descuento
    private static final double RECARGO_URGENCIA = 1.20; // +20% por entrega prioritaria

    /**
     * Calcula toda la cotizacion tecnica/ambiental de un vidrio
     * sostenible a partir de sus dimensiones y opciones de
     * fabricacion.
     *
     * IMPORTANTE: esta version reemplaza la formula generica por kg
     * que tenia antes (ver historial) por la formula real que ya
     * usa la vista previa en vivo de vidrio-personalizado-form.html
     * (calculo por m2, especifico por tipo de vidrio, con recargos
     * de tratamiento/acabado/perforaciones), para que lo que se
     * guarda coincida con lo que el usuario vio antes de enviar el
     * formulario.
     */
    public ResultadoAnalisisVidrio calcularCotizacionVidrio(VidrioSostenible datos) {
        ResultadoAnalisisVidrio resultado = new ResultadoAnalisisVidrio();

        double ancho = datos.getAncho();
        double alto = datos.getAlto();
        double espesor = datos.getEspesor();
        String tipoVidrio = normalizar(datos.getTipoVidrio());
        String tratamiento = normalizar(datos.getTratamiento());
        String acabado = normalizar(datos.getAcabadoBordes());
        boolean isEco = datos.isUsarVidrioReciclado();
        boolean isRenovable = datos.isEnergiaRenovable();

        double area = (ancho * alto) / CONVERSION_M2;
        double peso = area * espesor * factorDensidad(tipoVidrio);

        resultado.setAreaTotal(redondear(area));
        resultado.setPesoEstimado(redondear(peso));

        // --- Composicion de materiales (proporcional al peso; se reduce con reciclado) ---
        double proporcionMaterialNuevo = 1 - (isEco ? datos.getPorcentajeReciclado() / 100.0 : 0.0);
        resultado.setCantidadSilice(redondear(peso * PORC_SILICE * proporcionMaterialNuevo));
        resultado.setCantidadSosa(redondear(peso * PORC_SOSA * proporcionMaterialNuevo));
        resultado.setCantidadCaliza(redondear(peso * PORC_CALIZA * proporcionMaterialNuevo));
        resultado.setCantidadAlumina(redondear(peso * PORC_ALUMINA * proporcionMaterialNuevo));
        resultado.setEnergiaRequerida(redondear(peso * ENERGIA_POR_KG_KWH * (isRenovable ? 0.5 : 1.0)));

        // --- Huella de CO2 (formula del JS) ---
        double factorCO2 = isRenovable ? 0.6 : 1.1;
        if ("TempladoLaminado".equalsIgnoreCase(tipoVidrio)) factorCO2 *= 1.4;
        if ("Inteligente".equalsIgnoreCase(tipoVidrio) || "Fotovoltaico".equalsIgnoreCase(tipoVidrio)) factorCO2 *= 1.8;
        if (isEco) factorCO2 *= 0.85;
        double co2 = peso * factorCO2;
        if (datos.isCompensarCarbono()) {
            co2 = 0.0; // se asume compensacion total via creditos de carbono
        }
        resultado.setHuellaCarbono(redondear(co2));

        // --- Precio (formula del JS) ---
        double precioBaseM2 = precioBasePorM2(tipoVidrio);
        precioBaseM2 += recargoTratamiento(tratamiento);

        double precio = area * precioBaseM2 * (espesor / ESPESOR_BASE_NORMALIZACION_MM);

        double perimetroMetros = ((ancho + alto) * 2) / 100.0;
        if ("PulidoPlano".equalsIgnoreCase(acabado)) precio += perimetroMetros * COSTO_PULIDO_POR_METRO_LINEAL;
        if ("Biselado".equalsIgnoreCase(acabado)) precio += perimetroMetros * COSTO_BISELADO_POR_METRO_LINEAL;

        precio += datos.getPerforaciones() * COSTO_POR_PERFORACION;

        if (datos.isCompensarCarbono()) precio += RECARGO_COMPENSACION_CARBONO;
        if (isEco) precio *= DESCUENTO_RECICLADO;
        if (datos.isUrgente()) precio *= RECARGO_URGENCIA;

        resultado.setPrecioEstimado(redondear(precio));

        // --- Resistencia estimada (el JS no la calcula; se extiende aqui por tipo de vidrio) ---
        resultado.setResistenciaEstimada(redondear(resistenciaBase(tipoVidrio) + (datos.getCantidadCapas() - 1) * 10));

        resultado.setRecomendacion(generarRecomendacion(datos, resultado, tipoVidrio));

        return resultado;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private double factorDensidad(String tipoVidrio) {
        if ("Insulado".equalsIgnoreCase(tipoVidrio)) return 2.0;
        if ("Laminado".equalsIgnoreCase(tipoVidrio) || "TempladoLaminado".equalsIgnoreCase(tipoVidrio)) return 2.6;
        return 2.5; // Crudo, Templado, Inteligente, Fotovoltaico
    }

    private double precioBasePorM2(String tipoVidrio) {
        return switch (tipoVidrio) {
            case "Templado" -> PRECIO_M2_TEMPLADO;
            case "Laminado" -> PRECIO_M2_LAMINADO;
            case "TempladoLaminado" -> PRECIO_M2_TEMPLADO_LAMINADO;
            case "Insulado" -> PRECIO_M2_INSULADO;
            case "Inteligente" -> PRECIO_M2_INTELIGENTE;
            case "Fotovoltaico" -> PRECIO_M2_FOTOVOLTAICO;
            default -> PRECIO_M2_DEFAULT; // Crudo u otro no reconocido
        };
    }

    private double recargoTratamiento(String tratamiento) {
        if ("ControlSolar".equalsIgnoreCase(tratamiento) || "Antireflejo".equalsIgnoreCase(tratamiento)) {
            return RECARGO_CONTROL_SOLAR_ANTIREFLEJO;
        }
        if ("Serigrafiado".equalsIgnoreCase(tratamiento)) {
            return RECARGO_SERIGRAFIADO;
        }
        if ("TintadoGris".equalsIgnoreCase(tratamiento) || "TintadoBronce".equalsIgnoreCase(tratamiento)) {
            return RECARGO_TINTADO;
        }
        return 0;
    }

    private double resistenciaBase(String tipoVidrio) {
        return switch (tipoVidrio) {
            case "Templado" -> RESISTENCIA_BASE_MPA * 4.0;
            case "TempladoLaminado" -> RESISTENCIA_BASE_MPA * 5.0;
            case "Laminado" -> RESISTENCIA_BASE_MPA * 1.5;
            case "Insulado" -> RESISTENCIA_BASE_MPA * 1.8;
            case "Inteligente" -> RESISTENCIA_BASE_MPA * 1.5;
            case "Fotovoltaico" -> RESISTENCIA_BASE_MPA * 1.6;
            default -> RESISTENCIA_BASE_MPA; // Crudo
        };
    }

    /**
     * Recomendaciones basicas de espesor por uso (los rangos vienen
     * de los tooltips del propio formulario: Muebles 6-8mm, Ventanas
     * 6-10mm, Duchas 8-10mm, Barandas/Pisos 12-25mm).
     */
    private String validarEspesorParaUso(String usoPrincipal, double espesor) {
        if (usoPrincipal == null) return null;
        return switch (usoPrincipal) {
            case "Mobiliario" -> (espesor < 6 || espesor > 8)
                    ? "Para mobiliario se recomienda un espesor entre 6 y 8 mm." : null;
            case "Ventana" -> (espesor < 6 || espesor > 10)
                    ? "Para ventanas se recomienda un espesor entre 6 y 10 mm." : null;
            case "Ducha" -> (espesor < 8 || espesor > 10)
                    ? "Para divisiones de baño/ducha se recomienda un espesor entre 8 y 10 mm por seguridad." : null;
            case "Baranda", "Piso" -> (espesor < 12 || espesor > 25)
                    ? "Para barandas o pisos de vidrio se recomienda un espesor entre 12 y 25 mm." : null;
            default -> null;
        };
    }

    private String generarRecomendacion(VidrioSostenible datos, ResultadoAnalisisVidrio resultado, String tipoVidrio) {
        String alertaEspesor = validarEspesorParaUso(datos.getUsoPrincipal(), datos.getEspesor());
        if (alertaEspesor != null) {
            return alertaEspesor;
        }
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