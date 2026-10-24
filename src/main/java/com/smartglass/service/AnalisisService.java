package com.smartglass.service;

import com.smartglass.dto.PuntoCorte;
import com.smartglass.model.mongo.VidrioSostenible;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aisla TODA la logica matematica de eficiencia energetica y huella
 * de CO2 de SmartGlass, para que ni los controllers ni los servicios
 * de persistencia (PedidoService, VidrioSostenibleService) tengan
 * que conocer formulas ni constantes de proceso.
 *
 * CONTRATO DE FORMULAS CON EL FRONTEND (configurador / glass-preview.js):
 *   area m2   = (ancho_cm * alto_cm) / 10000
 *   peso kg   = area_m2 * espesor_mm * densidad(tipo)
 *               densidad: 2.5 base | 2.0 Insulado | 2.6 Laminado o Templado+Laminado
 *   co2 kg    = peso * factorCO2
 *   factorCO2 = (0.6 con energia renovable | 1.1 sin ella)
 *               * 1.4 si el tipo es Templado+Laminado
 *               * 1.8 si el tipo es Inteligente o Fotovoltaico
 *               * 0.85 si usa vidrio reciclado
 * Cualquier cambio en estas constantes debe reflejarse tambien en el
 * configurador del formulario (y viceversa) para que configurador,
 * detalle y factura muestren siempre el mismo numero.
 *
 * NOTA HISTORICA: las formulas de composicion, energia y precio son
 * una base razonable basada en vidrio flotado soda-lime; ajusta los
 * coeficientes (PORC_*, ENERGIA_POR_KG_KWH, PRECIO_*) a los valores
 * reales de tu proceso productivo cuando los tengas.
 *
 * CAMBIOS (auditoria de fugas), sin tocar constantes ni formulas del contrato:
 * - F25: la anatomia de la pieza cierra con el peso: reciclado (15%) + materia
 *   virgen (sílice, sosa, caliza, alumina) + "otros oxidos" (2% de la virgen).
 * - F27: cantidadCapas se fuerza a minimo 1 al calcular la resistencia.
 * - F45: rama ADITIVA de area: una pieza forma = "Personalizada" con poligono valido usa el area
 *   real (shoelace); todo lo demas sigue por caja envolvente, bit a bit como antes. No cambia
 *   constantes, densidades, factores, recargos ni umbrales.
 * - F13: calcularHuellaCarbonoItem usa factorDensidad(tipo) en vez de 2.5 fijo.
 * - F22: normalizarTipo tambien quita '/', para etiquetas como "Cubierta / mueble".
 */
@Service
public class AnalisisService {

    // --- Constantes fisicas compartidas ---
    private static final double DENSIDAD_VIDRIO = 2.5; // kg / (m2 * mm)  (densidad base)
    private static final double CONVERSION_M2 = 10000.0; // cm2 -> m2

    // --- Factores de emision de CO2 por proceso (kg CO2 / kg vidrio) ---
    // Usados SOLO por calcularHuellaCarbonoItem (productos de catalogo
    // en carrito/checkout). Las cotizaciones a medida usan el factorCO2
    // del contrato de formulas de la cabecera.
    private static final double FACTOR_TEMPLADO = 0.85;
    private static final double FACTOR_LAMINADO = 1.2;
    private static final double FACTOR_INSULADO = 1.5;
    private static final double FACTOR_DEFAULT = 1.0;

    // --- Composicion tipica de vidrio soda-lime (referencia industria) ---
    private static final double PORC_SILICE = 0.72;
    private static final double PORC_SOSA = 0.13;
    private static final double PORC_CALIZA = 0.09;
    private static final double PORC_ALUMINA = 0.04;
    private static final double PORC_OTROS = 0.02; // MgO, K2O, etc. (completa el 100% de la materia virgen)
    private static final double FRACCION_RECICLADO = 0.15; // "Reduce 15% huella" prometido por la UI

    // --- Energia y costo base ---
    private static final double ENERGIA_POR_KG_KWH = 2.5; // kWh por kg de vidrio fundido
    private static final double PRECIO_BASE_POR_KG = 4500.0; // COP por kg
    private static final double RESISTENCIA_BASE_MPA = 45.0; // vidrio recocido estandar

    // --- F45: limites del corte personalizado ---
    private static final int MIN_PUNTOS_CORTE = 3;
    private static final int MAX_PUNTOS_CORTE = 64;
    private static final double MAX_COORD_CORTE_CM = 10_000.0; // 100 m: cota de cordura, el editor trabaja en 300 x 200 cm

    /**
     * Calcula la huella de carbono de un item de pedido (productos de
     * catalogo en carrito/checkout) a partir de sus dimensiones y tipo
     * de proceso.
     * OJO: si el item proviene de una cotizacion a medida aprobada,
     * preferir el valor ya almacenado (huellaCarbono de la cotizacion)
     * en lugar de recalcular con estos factores de catalogo (F12: eso
     * se decide en PedidoService, que no vive aqui).
     */
    public double calcularHuellaCarbonoItem(Double ancho, Double alto, Double espesor, String tipo, int cantidad) {
        double anchoVal = (ancho != null) ? ancho : 0;
        double altoVal = (alto != null) ? alto : 0;
        double espesorVal = (espesor != null) ? espesor : 0;
        double area = (anchoVal * altoVal) / CONVERSION_M2;
        double peso = area * espesorVal * factorDensidad(normalizarTipo(tipo));
        double co2 = peso * obtenerFactorEmision(tipo) * cantidad;
        return Math.round(co2 * 100.0) / 100.0;
    }

    private double obtenerFactorEmision(String tipo) {
        if (tipo == null) return FACTOR_DEFAULT;
        return switch (normalizarTipo(tipo)) {
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
     * sostenible a partir de sus dimensiones y opciones de fabricacion.
     * La formula de CO2 es identica a la del configurador del frontend
     * (ver contrato en la cabecera de la clase).
     */
    public ResultadoAnalisisVidrio calcularCotizacionVidrio(VidrioSostenible datos) {
        ResultadoAnalisisVidrio resultado = new ResultadoAnalisisVidrio();

        double ancho = datos.getAncho();
        double alto = datos.getAlto();
        double espesor = datos.getEspesor();
        String tipoVidrio = normalizarTipo(datos.getTipoVidrio());
        String tratamiento = normalizarTipo(datos.getTratamiento());
        String acabado = normalizarTipo(datos.getAcabadoBordes());
        boolean isEco = datos.isUsarVidrioReciclado();
        boolean isRenovable = datos.isEnergiaRenovable();

        // F45: frontera del contrato. Solo esta rama bifurca el area; peso, CO2, intensidad, resistencia
        // y precio usan `area` exactamente igual que antes.
        List<PuntoCorte> pts = datos.getPuntosPersonalizados();
        boolean corteValido = "Personalizada".equalsIgnoreCase(datos.getForma())
                && pts != null && pts.size() >= MIN_PUNTOS_CORTE && pts.size() <= MAX_PUNTOS_CORTE
                && pts.stream().allMatch(p -> p != null
                        && Double.isFinite(p.getX()) && Double.isFinite(p.getY())
                        && p.getX() >= 0 && p.getY() >= 0
                        && p.getX() <= MAX_COORD_CORTE_CM && p.getY() <= MAX_COORD_CORTE_CM)
                && !autointersecta(pts);   // un poligono que se cruza no tiene area real: cae a bbox (el lado seguro)
        double areaPoligonalM2 = corteValido ? areaPoligono(pts) / CONVERSION_M2 : 0.0;
        boolean usaPoligono = corteValido && areaPoligonalM2 > 0.0;
        double area;
        if (usaPoligono) {
            area = areaPoligonalM2;                         // F45: tu corte real
            // El ancho/alto de la pieza es la caja envolvente del poligono: el cliente no puede fijar
            // medidas distintas a su corte (afectarian el perimetro del bisel/pulido y el boceto).
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (PuntoCorte p : pts) {
                minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            }
            ancho = maxX - minX;
            alto = maxY - minY;
            datos.setAncho(ancho);
            datos.setAlto(alto);
        } else {
            area = (ancho * alto) / CONVERSION_M2;          // ruta de siempre, intacta (red de seguridad)
        }
        resultado.setCorteAplicado(usaPoligono);
        double peso = area * espesor * factorDensidad(tipoVidrio);
        resultado.setAreaTotal(redondear(area));
        resultado.setPesoEstimado(redondear(peso));

        // --- Composicion de materiales (F25: la suma visible debe igualar el peso) ---
        // El formulario real solo tiene el checkbox "usar reciclado", sin
        // porcentaje: se aplica el mismo 15% que la propia UI promete
        // ("Reduce 15% huella"). peso = reciclado + virgen; la materia virgen
        // se reparte en silice/sosa/caliza/alumina (96%) + otros oxidos (2%
        // de la tabla soda-lime base, que suma 0,98 sin ellos).
        double recicladoKg = isEco ? redondear(peso * FRACCION_RECICLADO) : 0.0;
        double virgen = peso - recicladoKg;
        resultado.setCantidadSilice(redondear(virgen * PORC_SILICE));
        resultado.setCantidadSosa(redondear(virgen * PORC_SOSA));
        resultado.setCantidadCaliza(redondear(virgen * PORC_CALIZA));
        resultado.setCantidadAlumina(redondear(virgen * PORC_ALUMINA));
        resultado.setCantidadOtros(redondear(virgen * PORC_OTROS));
        resultado.setEnergiaRequerida(redondear(peso * ENERGIA_POR_KG_KWH * (isRenovable ? 0.5 : 1.0)));

        // --- Huella de CO2 (formula del JS / contrato) ---
        double factorCO2 = isRenovable ? 0.6 : 1.1;
        if ("templadolaminado".equals(tipoVidrio)) factorCO2 *= 1.4;
        if ("inteligente".equals(tipoVidrio) || "fotovoltaico".equals(tipoVidrio)) factorCO2 *= 1.8;
        if (isEco) factorCO2 *= 0.85;
        double co2 = peso * factorCO2;
        resultado.setHuellaCarbono(redondear(co2));

        // --- Precio (formula del JS) ---
        double precioBaseM2 = precioBasePorM2(tipoVidrio);
        precioBaseM2 += recargoTratamiento(tratamiento);
        double precio = area * precioBaseM2 * (espesor / ESPESOR_BASE_NORMALIZACION_MM);
        double perimetroMetros = ((ancho + alto) * 2) / 100.0;
        if ("pulidoplano".equals(acabado)) precio += perimetroMetros * COSTO_PULIDO_POR_METRO_LINEAL;
        if ("biselado".equals(acabado)) precio += perimetroMetros * COSTO_BISELADO_POR_METRO_LINEAL;
        precio += datos.getPerforaciones() * COSTO_POR_PERFORACION;
        if (datos.isCompensarCarbono()) precio += RECARGO_COMPENSACION_CARBONO;
        if (isEco) precio *= DESCUENTO_RECICLADO;
        if (datos.isUrgente()) precio *= RECARGO_URGENCIA;
        resultado.setPrecioEstimado(redondear(precio));

        // --- Resistencia estimada (el JS no la calcula; se extiende aqui por tipo) ---
        // F27: una pieza siempre tiene al menos 1 capa; con 0 la formula restaba 10 MPa (Templado 170 en vez de 180).
        int capas = Math.max(1, datos.getCantidadCapas());
        resultado.setResistenciaEstimada(redondear(resistenciaBase(tipoVidrio) + (capas - 1) * 10.0));
        resultado.setRecomendacion(generarRecomendacion(datos, resultado));
        return resultado;
    }

    /** F45: area real de un poligono (shoelace/Gauss). Coordenadas en cm -> cm2. */
    private static double areaPoligono(List<PuntoCorte> pts) {
        if (pts == null || pts.size() < 3) return 0.0;
        double s = 0.0;
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            PuntoCorte a = pts.get(i), b = pts.get((i + 1) % n);
            s += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return Math.abs(s) / 2.0;
    }

    /** F45: true si dos aristas NO contiguas se cruzan o se tocan (el area por shoelace dejaria de ser real). */
    private static boolean autointersecta(List<PuntoCorte> p) {
        int n = p.size();
        for (int i = 0; i < n; i++) {
            PuntoCorte a = p.get(i), b = p.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (j == i + 1 || (i == 0 && j == n - 1)) continue;   // aristas contiguas
                PuntoCorte c = p.get(j), d = p.get((j + 1) % n);
                if (segmentosSeCruzan(a, b, c, d)) return true;
            }
        }
        return false;
    }

    private static boolean segmentosSeCruzan(PuntoCorte a, PuntoCorte b, PuntoCorte c, PuntoCorte d) {
        double o1 = orientacion(a, b, c), o2 = orientacion(a, b, d);
        double o3 = orientacion(c, d, a), o4 = orientacion(c, d, b);
        if (((o1 > 0 && o2 < 0) || (o1 < 0 && o2 > 0)) && ((o3 > 0 && o4 < 0) || (o3 < 0 && o4 > 0))) return true;
        return (o1 == 0 && enSegmento(a, b, c)) || (o2 == 0 && enSegmento(a, b, d))
            || (o3 == 0 && enSegmento(c, d, a)) || (o4 == 0 && enSegmento(c, d, b));
    }

    private static double orientacion(PuntoCorte a, PuntoCorte b, PuntoCorte c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    /** c es colineal con a-b; true si ademas cae dentro de su caja. */
    private static boolean enSegmento(PuntoCorte a, PuntoCorte b, PuntoCorte c) {
        return Math.min(a.getX(), b.getX()) <= c.getX() && c.getX() <= Math.max(a.getX(), b.getX())
            && Math.min(a.getY(), b.getY()) <= c.getY() && c.getY() <= Math.max(a.getY(), b.getY());
    }

    /**
     * Normaliza etiquetas que vienen del formulario: minusculas y sin
     * espacios, '+', '_', '-' ni '/'. Asi "Templado + Laminado",
     * "Templado+Laminado" y "TempladoLaminado" coinciden siempre y el
     * configurador y el detalle jamas divergen por un texto.
     */
    private String normalizarTipo(String valor) {
        if (valor == null) return "";
        return valor.toLowerCase().replaceAll("[\\s+_\\-/]", "");
    }

    private double factorDensidad(String tipoNormalizado) {
        if ("insulado".equals(tipoNormalizado)) return 2.0;
        if ("laminado".equals(tipoNormalizado) || "templadolaminado".equals(tipoNormalizado)) return 2.6;
        return DENSIDAD_VIDRIO; // 2.5: Crudo, Templado, Inteligente, Fotovoltaico
    }

    private double precioBasePorM2(String tipoNormalizado) {
        return switch (tipoNormalizado) {
            case "templado" -> PRECIO_M2_TEMPLADO;
            case "laminado" -> PRECIO_M2_LAMINADO;
            case "templadolaminado" -> PRECIO_M2_TEMPLADO_LAMINADO;
            case "insulado" -> PRECIO_M2_INSULADO;
            case "inteligente" -> PRECIO_M2_INTELIGENTE;
            case "fotovoltaico" -> PRECIO_M2_FOTOVOLTAICO;
            default -> PRECIO_M2_DEFAULT; // Crudo u otro no reconocido
        };
    }

    private double recargoTratamiento(String tratamientoNormalizado) {
        if ("controlsolar".equals(tratamientoNormalizado) || "antireflejo".equals(tratamientoNormalizado)) {
            return RECARGO_CONTROL_SOLAR_ANTIREFLEJO;
        }
        if ("serigrafiado".equals(tratamientoNormalizado)) {
            return RECARGO_SERIGRAFIADO;
        }
        if ("tintadogris".equals(tratamientoNormalizado) || "tintadobronce".equals(tratamientoNormalizado)) {
            return RECARGO_TINTADO;
        }
        return 0;
    }

    private double resistenciaBase(String tipoNormalizado) {
        return switch (tipoNormalizado) {
            case "templado" -> RESISTENCIA_BASE_MPA * 4.0;
            case "templadolaminado" -> RESISTENCIA_BASE_MPA * 5.0;
            case "laminado" -> RESISTENCIA_BASE_MPA * 1.5;
            case "insulado" -> RESISTENCIA_BASE_MPA * 1.8;
            case "inteligente" -> RESISTENCIA_BASE_MPA * 1.5;
            case "fotovoltaico" -> RESISTENCIA_BASE_MPA * 1.6;
            default -> RESISTENCIA_BASE_MPA; // Crudo
        };
    }

    /**
     * Recomendaciones basicas de espesor por uso (rangos de los tooltips
     * del formulario: Muebles 6-8mm, Ventanas 6-10mm, Duchas 8-10mm,
     * Barandas/Pisos 12-25mm).
     * F22: los case deben coincidir con el value REAL del <select> de uso
     * (normalizado). Verifica en vidrio-personalizado-form.html y, si el
     * value es otro literal, agrega aqui ese caso.
     */
    private String validarEspesorParaUso(String usoPrincipal, double espesor) {
        if (usoPrincipal == null) return null;
        return switch (normalizarTipo(usoPrincipal)) {
            case "mobiliario", "muebles" -> (espesor < 6 || espesor > 8)
                    ? "Para mobiliario se recomienda un espesor entre 6 y 8 mm. " : null;
            case "ventana" -> (espesor < 6 || espesor > 10)
                    ? "Para ventanas se recomienda un espesor entre 6 y 10 mm. " : null;
            case "ducha" -> (espesor < 8 || espesor > 10)
                    ? "Para divisiones de baño/ducha se recomienda un espesor entre 8 y 10 mm por seguridad. " : null;
            case "baranda", "piso" -> (espesor < 12 || espesor > 25)
                    ? "Para barandas o pisos de vidrio se recomienda un espesor entre 12 y 25 mm. " : null;
            default -> null;
        };
    }

    private String generarRecomendacion(VidrioSostenible datos, ResultadoAnalisisVidrio resultado) {
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