package com.smartglass.model.mongo;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Version real de ItemCarrito (la que habias subido), movida de
 * com.smartglass.model a com.smartglass.model.mongo. Unico cambio de
 * fondo: el campo "producto" pasa de Producto a CatalogoProducto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemCarrito implements Serializable {

    private static final long serialVersionUID = 1L;

    private CatalogoProducto producto;
    private int cantidad;

    public ItemCarrito() {}

    public ItemCarrito(CatalogoProducto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    // --- Metodos de logica ---

    public void incrementarCantidad(int cantidad) {
        this.cantidad += cantidad;
    }

    public double getSubtotal() {
        if (producto == null) return 0;
        return producto.getPrecio() * cantidad;
    }

    // --- Getters y Setters ---

    public CatalogoProducto getProducto() { return producto; }
    public void setProducto(CatalogoProducto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    // --- Formato para Thymeleaf ---

    public String getSubtotalFormateado() {
        return String.format("%,.0f", getSubtotal());
    }
}
