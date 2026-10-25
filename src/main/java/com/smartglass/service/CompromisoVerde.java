package com.smartglass.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * T3 · Sello "Compromiso verde" de una obra: puntos por las decisiones sostenibles del cliente.
 *
 * - NO forma parte del contrato de calculo: no cambia CO2, intensidad, nivel (12/20) ni precio.
 *   Es un reconocimiento de lo que el cliente eligio, separado del semaforo de impacto (F24).
 * - Fuente unica de pesos y cortes: el configurador los recibe del servidor (model "compromisoVerde")
 *   para calcular el sello en vivo con las mismas cifras.
 * - Los compromisos nuevos de T3 (retoma, empaque, entrega consolidada, retazos) no tienen costo.
 */
public final class CompromisoVerde {

    /** Puntos por opcion (suman 100). Claves = nombres de los campos del formulario. */
    public static final Map<String, Integer> PESOS;
    static {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("usarVidrioReciclado", 20);
        p.put("energiaRenovable", 20);
        p.put("compensarCarbono", 15);
        p.put("retomaVidrio", 15);
        p.put("empaqueReutilizable", 10);
        p.put("entregaConsolidada", 10);
        p.put("usarRetazos", 10);
        PESOS = java.util.Collections.unmodifiableMap(p);
    }

    /** Puntos minimos de cada sello (de mayor a menor). Por debajo de BRONCE no hay sello. */
    public static final int ORO = 85, PLATA = 60, BRONCE = 35;

    private CompromisoVerde() { }

    public static int puntos(boolean reciclado, boolean renovable, boolean compensar,
                             boolean retoma, boolean empaque, boolean consolidada, boolean retazos) {
        int s = 0;
        if (reciclado) s += PESOS.get("usarVidrioReciclado");
        if (renovable) s += PESOS.get("energiaRenovable");
        if (compensar) s += PESOS.get("compensarCarbono");
        if (retoma) s += PESOS.get("retomaVidrio");
        if (empaque) s += PESOS.get("empaqueReutilizable");
        if (consolidada) s += PESOS.get("entregaConsolidada");
        if (retazos) s += PESOS.get("usarRetazos");
        return s;
    }

    /** "ORO", "PLATA", "BRONCE" o "" (sin sello). */
    public static String nivel(int puntos) {
        if (puntos >= ORO) return "ORO";
        if (puntos >= PLATA) return "PLATA";
        if (puntos >= BRONCE) return "BRONCE";
        return "";
    }

    /** Lo que necesita el configurador para calcular el sello en vivo. */
    public static Map<String, Object> paraVista() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pesos", PESOS);
        m.put("oro", ORO);
        m.put("plata", PLATA);
        m.put("bronce", BRONCE);
        return m;
    }
}
