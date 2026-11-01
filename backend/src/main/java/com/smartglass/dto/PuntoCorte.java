package com.smartglass.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F45: vertice de un corte personalizado, en cm relativos al lienzo del editor.
 *
 * Formato de texto compacto "x,y;x,y;..." (punto decimal, 2 decimales): es el que viaja en el
 * campo oculto del formulario HTML y en el atributo data-puntos del detalle. Spring no puede
 * enlazar un String a List<PuntoCorte>, por eso el DTO de entrada tiene ademas setPuntosTexto().
 */
public class PuntoCorte {

    /** Tope duro del formato (el editor limita a 64 vertices). */
    private static final int MAX_PUNTOS_TEXTO = 64;
    private static final int MAX_LONGITUD_TEXTO = 4000;

    private double x;
    private double y;

    public PuntoCorte() {
    }

    public PuntoCorte(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    /** "10,20;30,40" -> lista. Cualquier error de formato devuelve lista VACIA (el servidor cae a bbox). */
    public static List<PuntoCorte> desdeTexto(String texto) {
        List<PuntoCorte> puntos = new ArrayList<>();
        if (texto == null || texto.isBlank() || texto.length() > MAX_LONGITUD_TEXTO) return puntos;
        String[] pares = texto.trim().split(";");
        if (pares.length > MAX_PUNTOS_TEXTO) return new ArrayList<>();
        for (String par : pares) {
            String[] xy = par.trim().split(",");
            if (xy.length != 2) return new ArrayList<>();
            try {
                double x = Double.parseDouble(xy[0].trim());
                double y = Double.parseDouble(xy[1].trim());
                if (!Double.isFinite(x) || !Double.isFinite(y)) return new ArrayList<>();
                puntos.add(new PuntoCorte(x, y));
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
        }
        return puntos;
    }

    /** Lista -> "10,20;30,40" ("" si no hay puntos). Independiente del idioma del servidor. */
    public static String aTexto(List<PuntoCorte> puntos) {
        if (puntos == null || puntos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PuntoCorte p : puntos) {
            if (p == null) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(numero(p.getX())).append(',').append(numero(p.getY()));
        }
        return sb.toString();
    }

    private static String numero(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}