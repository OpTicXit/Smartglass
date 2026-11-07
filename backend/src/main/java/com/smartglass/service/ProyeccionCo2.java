package com.smartglass.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Proyeccion del CO2 total estimado a partir de una historia diaria (kg CO2e por dia).
 *
 * Minimos cuadrados con tendencia lineal y terminos de Fourier (semanal con 21+ dias, anual con 730+).
 * Misma formula que "mineria de datos/proyeccion_co2.py": si se cambia una, se cambia la otra.
 * Nunca proyecta mas lejos que la historia disponible y nunca devuelve valores negativos.
 */
public final class ProyeccionCo2 {

    private ProyeccionCo2() {}

    /** Valor de un dia futuro y su rango probable. */
    public record Banda(double y, double lo, double hi) {}

    /** Acumulado desde hoy hasta el horizonte (sin lo ya emitido). */
    public record Horizonte(String clave, int dias, double total, double lo, double hi) {}

    /** historia termina en fin (fin = null si las fechas son ilustrativas). */
    public record Proyeccion(LocalDate fin, List<Double> historia, List<Banda> futuro, List<Horizonte> horizontes) {}

    static final String[] CLAVES = { "semana", "mes", "cinco_meses", "anio", "cinco_anios" };
    static final int[] DIAS = { 7, 30, 150, 365, 1826 };
    private static final int MIN_DIAS = 7;
    private static final double Z = 1.96;

    public static Proyeccion proyectar(List<Double> historiaDiaria, LocalDate fin) {
        List<Double> historia = historiaDiaria == null ? List.of() : List.copyOf(historiaDiaria);
        int n = historia.size();
        if (n < MIN_DIAS) return new Proyeccion(fin, historia, List.of(), List.of());

        int horizonteMax = 0;
        for (int d : DIAS) if (d <= n) horizonteMax = d;

        int p = variables(0, n).length;
        double[][] xtx = new double[p][p];
        double[] xty = new double[p];
        for (int t = 0; t < n; t++) {
            double[] x = variables(t, n);
            double y = historia.get(t);
            for (int i = 0; i < p; i++) {
                xty[i] += x[i] * y;
                for (int j = 0; j < p; j++) xtx[i][j] += x[i] * x[j];
            }
        }
        double[] beta = resolver(xtx, xty);

        double sse = 0;
        for (int t = 0; t < n; t++) {
            double r = historia.get(t) - punto(beta, variables(t, n));
            sse += r * r;
        }
        double sigma = Math.sqrt(sse / Math.max(1, n - p));

        List<Banda> futuro = new ArrayList<>(horizonteMax);
        double[] acumulado = new double[horizonteMax + 1];
        for (int h = 1; h <= horizonteMax; h++) {
            double estimado = punto(beta, variables(n - 1 + h, n));
            double semiancho = Z * sigma * Math.sqrt(1 + (double) h / n);
            double y = Math.max(0, estimado);
            futuro.add(new Banda(y, Math.max(0, estimado - semiancho), Math.max(0, estimado + semiancho)));
            acumulado[h] = acumulado[h - 1] + y;
        }

        List<Horizonte> horizontes = new ArrayList<>();
        for (int k = 0; k < DIAS.length && DIAS[k] <= n; k++) {
            int d = DIAS[k];
            double total = acumulado[d];
            double semiancho = Z * sigma * Math.sqrt(d) * (1 + (double) d / n);
            horizontes.add(new Horizonte(CLAVES[k], d, total, Math.max(0, total - semiancho), total + semiancho));
        }
        return new Proyeccion(fin, historia, futuro, horizontes);
    }

    /** 1, t, Fourier semanal (n >= 21) y anual (n >= 730), k = 1 y 2. */
    static double[] variables(int t, int n) {
        boolean semanal = n >= 21, anual = n >= 730;
        double[] x = new double[2 + (semanal ? 4 : 0) + (anual ? 4 : 0)];
        int i = 0;
        x[i++] = 1;
        x[i++] = t;
        if (semanal) for (int k = 1; k <= 2; k++) {
            double a = 2 * Math.PI * k * t / 7.0;
            x[i++] = Math.sin(a);
            x[i++] = Math.cos(a);
        }
        if (anual) for (int k = 1; k <= 2; k++) {
            double a = 2 * Math.PI * k * t / 365.25;
            x[i++] = Math.sin(a);
            x[i++] = Math.cos(a);
        }
        return x;
    }

    private static double punto(double[] beta, double[] x) {
        double s = 0;
        for (int i = 0; i < beta.length; i++) s += beta[i] * x[i];
        return s;
    }

    /** Eliminacion gaussiana con pivote parcial; un pivote nulo (columna sin informacion) deja ese coeficiente en 0. */
    static double[] resolver(double[][] a0, double[] b0) {
        int p = b0.length;
        double[][] a = new double[p][];
        for (int i = 0; i < p; i++) a[i] = a0[i].clone();
        double[] b = b0.clone();
        for (int c = 0; c < p; c++) {
            int piv = c;
            for (int r = c + 1; r < p; r++) if (Math.abs(a[r][c]) > Math.abs(a[piv][c])) piv = r;
            double[] fila = a[c]; a[c] = a[piv]; a[piv] = fila;
            double tmp = b[c]; b[c] = b[piv]; b[piv] = tmp;
            if (Math.abs(a[c][c]) < 1e-12) continue;
            for (int r = c + 1; r < p; r++) {
                double f = a[r][c] / a[c][c];
                if (f == 0) continue;
                for (int k = c; k < p; k++) a[r][k] -= f * a[c][k];
                b[r] -= f * b[c];
            }
        }
        double[] beta = new double[p];
        for (int c = p - 1; c >= 0; c--) {
            if (Math.abs(a[c][c]) < 1e-12) { beta[c] = 0; continue; }
            double s = b[c];
            for (int k = c + 1; k < p; k++) s -= a[c][k] * beta[k];
            beta[c] = s / a[c][c];
        }
        return beta;
    }
}
