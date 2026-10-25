package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
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

    // T2 comercio (stock, ofertas, balanceo). Tambien nullable y aditivos.
    // D1: el stock SOLO baja con compras reales (StockService, al confirmar el pedido).
    private Integer stock;                // null = sin control de inventario (no hay badges)
    private Double precioOferta;          // precio con descuento; solo cuenta si la oferta esta vigente
    private LocalDate ofertaInicio;       // inclusive; null = desde ya
    private LocalDate ofertaFin;          // inclusive; null = sin fecha de fin
    private boolean ofertaActiva;
    // D3: el balanceo de precios guarda el precio previo para poder revertirlo
    private Double precioAnterior;
    private String esquemaPrecio;         // "T2-v1" = balanceado; "manual" = lo fijo el admin

    public static final String CATEGORIA_VIDRIO_PLANO = "vidrio-plano";
    public static final String CATEGORIA_ESCULTURA = "escultura";
    /** Zona horaria de las fechas de oferta (la tienda vende en Colombia). */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** Umbral de "ultimas unidades" (stock > 0 y menor que esto). Lo fija StockService desde la configuracion. */
    private static volatile int umbralStockBajo = 5;
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

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Double getPrecioOferta() { return precioOferta; }
    public void setPrecioOferta(Double precioOferta) { this.precioOferta = precioOferta; }

    public LocalDate getOfertaInicio() { return ofertaInicio; }
    public void setOfertaInicio(LocalDate ofertaInicio) { this.ofertaInicio = ofertaInicio; }

    public LocalDate getOfertaFin() { return ofertaFin; }
    public void setOfertaFin(LocalDate ofertaFin) { this.ofertaFin = ofertaFin; }

    public boolean isOfertaActiva() { return ofertaActiva; }
    public void setOfertaActiva(boolean ofertaActiva) { this.ofertaActiva = ofertaActiva; }

    public Double getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(Double precioAnterior) { this.precioAnterior = precioAnterior; }

    public String getEsquemaPrecio() { return esquemaPrecio; }
    public void setEsquemaPrecio(String esquemaPrecio) { this.esquemaPrecio = esquemaPrecio; }

    public static int getUmbralStockBajo() { return umbralStockBajo; }
    public static void setUmbralStockBajo(int umbral) { umbralStockBajo = Math.max(1, umbral); }

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
        return formatear(precio);
    }

    /** Separador de miles es-CO fijo (D2), igual que getPrecioFormateado. */
    private static String formatear(double valor) {
        return String.format("%,.0f", valor).replace(",", ".");
    }

    // --- T2: stock ---

    public boolean isStockControlado() { return stock != null; }

    /** Agotado: hay control de inventario y no queda ninguna unidad. */
    public boolean isAgotado() { return stock != null && stock <= 0; }

    /** "Ultimas unidades": veraz, sale del stock real restante (D1), nunca de un contador simulado. */
    public boolean isUltimasUnidades() { return stock != null && stock > 0 && stock < umbralStockBajo; }

    // --- T2: ofertas ---

    /** La oferta cuenta si esta activa, abarata el precio y hoy cae dentro de sus fechas. */
    public boolean isOfertaVigente() {
        if (!ofertaActiva || precioOferta == null || precioOferta <= 0 || precioOferta >= precio) return false;
        LocalDate hoy = LocalDate.now(ZONA);
        return (ofertaInicio == null || !hoy.isBefore(ofertaInicio))
                && (ofertaFin == null || !hoy.isAfter(ofertaFin));
    }

    /** Lo que se cobra hoy: la oferta vigente o el precio de lista. */
    public double getPrecioVigente() { return isOfertaVigente() ? precioOferta : precio; }

    public String getPrecioVigenteFormateado() { return formatear(getPrecioVigente()); }

    /** Descuento entero (por ejemplo 15 para -15 %); 0 sin oferta vigente. */
    public int getDescuentoPorcentaje() {
        if (!isOfertaVigente() || precio <= 0) return 0;
        return (int) Math.round((1 - precioOferta / precio) * 100);
    }

    /**
     * Copia que se guarda dentro del carrito: lleva el precio vigente como precio, asi
     * el subtotal del carrito y el DetallePedido cobran la oferta sin tocar PedidoService.
     */
    public CatalogoProducto copiaParaCarrito() {
        CatalogoProducto c = new CatalogoProducto(nombre, imagen, getPrecioVigente(), codigo,
                descripcion, tipo, ancho, alto, espesor);
        c.setId(id);
        c.setCategoria(categoria);
        c.setTamano(tamano);
        c.setImagenPrincipal(imagenPrincipal);
        return c;
    }

    public String getDescripcionCorta() {
        if (descripcion != null && descripcion.length() > 100) {
            return descripcion.substring(0, 97) + "...";
        }
        return descripcion;
    }
}
