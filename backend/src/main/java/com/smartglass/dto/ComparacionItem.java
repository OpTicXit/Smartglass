package com.smartglass.dto;

import com.smartglass.model.mongo.CatalogoProducto;

public class ComparacionItem {

    private final CatalogoProducto producto;
    private final double promedioCalificacion;
    private final String estrellasPromedio;
    private final int totalResenas;

    public ComparacionItem(CatalogoProducto producto, double promedioCalificacion, int totalResenas) {
        this.producto = producto;
        this.promedioCalificacion = promedioCalificacion;
        this.totalResenas = totalResenas;
        int estrellasLlenas = (int) Math.round(promedioCalificacion);
        this.estrellasPromedio = "★".repeat(estrellasLlenas) + "☆".repeat(5 - estrellasLlenas);
    }

    public CatalogoProducto getProducto() { return producto; }
    public double getPromedioCalificacion() { return promedioCalificacion; }
    public String getEstrellasPromedio() { return estrellasPromedio; }
    public int getTotalResenas() { return totalResenas; }
}