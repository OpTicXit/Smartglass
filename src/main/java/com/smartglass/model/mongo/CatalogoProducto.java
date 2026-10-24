package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

    // T2 catalogo ampliado. Todos nullable y aditivos: los documentos que ya
    // existen no los tienen y se leen como vidrio plano (ver getCategoriaEfectiva).
    private String categoria;             // null o "vidrio-plano" | "escultura"
    private String tamano;                // "grande" | "mediana" | "pequena" (esculturas)
    private String imagenPrincipal;       // foto realista en /img/; solo se llena si el archivo existe
    private List<String> galeria;         // vistas extra en /img/; idem

    public static final String CATEGORIA_VIDRIO_PLANO = "vidrio-plano";
    public static final String CATEGORIA_ESCULTURA = "escultura";
    /** F18: si no hay ninguna imagen se muestra el isotipo, nunca un hueco roto. */
    public static final String IMAGEN_RESPALDO = "logo-icono.png";

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

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }

    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }

    public List<String> getGaleria() { return galeria; }
    public void setGaleria(List<String> galeria) { this.galeria = galeria; }

    // --- Metodos de utilidad ---

    /** Sin categoria guardada = vidrio plano (todo el catalogo anterior a T2). */
    public String getCategoriaEfectiva() {
        return categoria == null || categoria.isBlank() ? CATEGORIA_VIDRIO_PLANO : categoria;
    }

    public boolean isEscultura() {
        return CATEGORIA_ESCULTURA.equals(getCategoriaEfectiva());
    }

    /** Nombre legible del tamano ("pequena" -> "Pequeña"); null si no tiene. */
    public String getTamanoEtiqueta() {
        if (tamano == null) return null;
        return switch (tamano) {
            case "grande" -> "Grande";
            case "mediana" -> "Mediana";
            case "pequena" -> "Pequeña";
            default -> tamano;
        };
    }

    /**
     * Ruta de la imagen a mostrar: la foto realista si ya existe, si no la
     * imagen de siempre y, si no hay ninguna, el isotipo (F18).
     */
    public String getImagenUrl() {
        String archivo = imagenPrincipal != null && !imagenPrincipal.isBlank() ? imagenPrincipal
                : (imagen != null && !imagen.isBlank() ? imagen : IMAGEN_RESPALDO);
        return "/img/" + archivo;
    }

    /** true cuando se esta mostrando el respaldo: la vista lo presenta como "foto en preparacion". */
    public boolean isImagenPendiente() {
        return (imagenPrincipal == null || imagenPrincipal.isBlank()) && (imagen == null || imagen.isBlank());
    }

    /** Imagen principal + galeria, en ese orden y sin repetidos (para el detalle). */
    public List<String> getImagenesUrl() {
        List<String> todas = new ArrayList<>();
        todas.add(getImagenUrl());
        if (galeria != null) {
            for (String g : galeria) {
                String url = "/img/" + g;
                if (g != null && !g.isBlank() && !todas.contains(url)) todas.add(url);
            }
        }
        return todas;
    }

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
