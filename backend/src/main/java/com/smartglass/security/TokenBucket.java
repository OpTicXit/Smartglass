package com.smartglass.security;

/**
 * F41: cubeta de tokens sin dependencias externas, usada por RateLimitFilter.
 * Recarga continua (greedy): los tokens se reponen proporcionalmente al tiempo
 * transcurrido dentro de la ventana, nunca por encima de la capacidad.
 * El tiempo lo aporta el llamante (System.currentTimeMillis()) para que filtro
 * y cubeta compartan el mismo reloj.
 */
public class TokenBucket {

    private final int capacidad;
    private final double tokensPorMilisegundo;
    private double tokens;
    private long ultimaRecarga;
    private long ultimoUso;

    public TokenBucket(int capacidad, long ventanaMs, long ahora) {
        if (capacidad <= 0 || ventanaMs <= 0) {
            throw new IllegalArgumentException("capacidad y ventana deben ser > 0");
        }
        this.capacidad = capacidad;
        this.tokensPorMilisegundo = capacidad / (double) ventanaMs;
        this.tokens = capacidad;
        this.ultimaRecarga = ahora;
        this.ultimoUso = ahora;
    }

    /** Consume 1 ficha en el instante `ahora`. false si no hay fichas. */
    public synchronized boolean tryConsume(long ahora) {
        recargar(ahora);
        this.ultimoUso = ahora;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /** Segundos que faltan para tener 1 ficha libre (mínimo 1, para Retry-After). */
    public synchronized long segundosParaReintentar(long ahora) {
        recargar(ahora);
        this.ultimoUso = ahora;
        if (tokens >= 1.0) return 0L;
        long ms = (long) Math.ceil((1.0 - tokens) / tokensPorMilisegundo);
        return Math.max(1L, ms / 1000L);
    }

    /** Para la purga por inactividad del RateLimitFilter. */
    public synchronized long getUltimoUso() {
        return ultimoUso;
    }

    private void recargar(long ahora) {
        long dt = ahora - ultimaRecarga;
        if (dt > 0) {
            tokens = Math.min(capacidad, tokens + dt * tokensPorMilisegundo);
            ultimaRecarga = ahora;
        } else if (dt < 0) { // reloj hacia atrás: no regalar fichas
            ultimaRecarga = ahora;
        }
    }
}