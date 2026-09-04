package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Catalogo de productos de SmartGlass, almacenado en MongoDB.
 * Renombrado desde "Producto" a "CatalogoProducto" para reflejar
 * que esta clase vive en la capa documental (catalogo), separada
 * de las entidades relacionales de MySQL.
 */
@Document(collection = "catalogo_productos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogoProducto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private String nombre;
    private String imagen;
    private double precio;

    private String codigo;
    private String descripcion;
    private String tipo;

    private Double ancho;
    private Double alto;
    private Double espesor;

    public CatalogoProducto() {
    }

    public CatalogoProducto(String nombre, String imagen, Double precio, String codigo,
                             String descripcion, String tipo, Double ancho, Double alto, Double espesor) {
        this.nombre = nombre;
        this.imagen = imagen;
        this.precio = precio;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.ancho = ancho;
        this.alto = alto;
        this.espesor = espesor;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getAncho() { return ancho; }
    public void setAncho(Double ancho) { this.ancho = ancho; }

    public Double getAlto() { return alto; }
    public void setAlto(Double alto) { this.alto = alto; }

    public Double getEspesor() { return espesor; }
    public void setEspesor(Double espesor) { this.espesor = espesor; }

    // --- Metodos de utilidad ---

    public String getPrecioFormateado() {
        return String.format("%,.0f", precio).replace(",", ".");
    }

    public String getDescripcionCorta() {
        if (descripcion != null && descripcion.length() > 100) {
            return descripcion.substring(0, 97) + "...";
        }
        return descripcion;
    }
}
