package com.smartglass.service;

/**
 * T5 · Funciones que dependen del plan SaaS. Todo lo que no esta aqui es el nucleo y va en
 * ambos planes (tienda, carrito, pago, configurador con las 5 formas clasicas, cotizaciones con
 * aprobacion, huella de CO2 por pieza, notificaciones, comparador, resenas, guia y accesibilidad).
 *
 * Criterio (estudio de mercado del panel de superadmin): el plan Basico cubre lo que necesita un
 * local pequeno o poco estructurado para vender y cotizar en linea; el Avanzado suma las
 * herramientas de una vidrieria estructurada (geometria compleja, documentacion ambiental,
 * promociones, varios idiomas y cifras publicas).
 * Los textos viven en messages*.properties: funcion.<NOMBRE>.titulo / .desc / .porque
 */
public enum Funcion {
    FORMAS_AVANZADAS("ph-shapes"),
    CORTE_PERSONALIZADO("ph-pencil-ruler"),
    COMPROMISOS_VERDES("ph-hand-heart"),
    FICHA_AMBIENTAL("ph-file-text"),
    OFERTAS_PROGRAMADAS("ph-tag"),
    CIFRAS_PUBLICAS("ph-chart-line-up"),
    MULTI_IDIOMA("ph-translate");

    private final String icono;

    Funcion(String icono) { this.icono = icono; }

    public String getIcono() { return icono; }
    public String getClave() { return name(); }

    /** Todas las funciones de esta lista vienen en el plan Avanzado y ninguna en el Basico. */
    public boolean incluidaEn(String plan) { return PlanService.AVANZADO.equals(plan); }
}
