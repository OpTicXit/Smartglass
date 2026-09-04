package com.smartglass.model.mysql;

import com.smartglass.model.mongo.CatalogoProducto;
import jakarta.persistence.*;

/**
 * AJUSTE (catalogo global): DetallePedido es una entidad relacional
 * (MySQL), pero el catalogo de productos (CatalogoProducto) vive en
 * MongoDB. JPA no puede mapear un @ManyToOne cruzando motores de
 * base de datos, asi que aqui se guarda una "foto" (snapshot) de los
 * datos del producto en el momento de la compra -- practica comun en
 * lineas de pedido, ademas de evitar que el precio de un pedido ya
 * facturado cambie si el precio del catalogo cambia despues.
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Id del documento CatalogoProducto en Mongo (ya no es una FK real)
    @Column(name = "producto_id", nullable = false)
    private String productoId;

    @Column(name = "nombre_producto")
    private String nombreProducto;

    @Column(name = "imagen_producto")
    private String imagenProducto;

    private int cantidad;

    @Column(name = "precio_unitario")
    private double precioUnitario;

    private double subtotal;

    @Column(name = "ancho_personalizado")
    private Double anchoPersonalizado;

    @Column(name = "alto_personalizado")
    private Double altoPersonalizado;

    @Column(name = "espesor_personalizado")
    private Double espesorPersonalizado;

    @Column(name = "notas_personalizacion", length = 1000)
    private String notasPersonalizacion;

    public DetallePedido() {
    }

    public DetallePedido(CatalogoProducto producto, int cantidad) {
        this.productoId = producto.getId();
        this.nombreProducto = producto.getNombre();
        this.imagenProducto = producto.getImagen();
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        this.calcularSubtotal();
    }

    // --- Logica de negocio ---

    public void calcularSubtotal() {
        this.subtotal = this.precioUnitario * this.cantidad;
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getImagenProducto() { return imagenProducto; }
    public void setImagenProducto(String imagenProducto) { this.imagenProducto = imagenProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.calcularSubtotal();
    }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getSubtotal() { return subtotal; }

    public Double getAnchoPersonalizado() { return anchoPersonalizado; }
    public void setAnchoPersonalizado(Double anchoPersonalizado) { this.anchoPersonalizado = anchoPersonalizado; }

    public Double getAltoPersonalizado() { return altoPersonalizado; }
    public void setAltoPersonalizado(Double altoPersonalizado) { this.altoPersonalizado = altoPersonalizado; }

    public Double getEspesorPersonalizado() { return espesorPersonalizado; }
    public void setEspesorPersonalizado(Double espesorPersonalizado) { this.espesorPersonalizado = espesorPersonalizado; }

    public String getNotasPersonalizacion() { return notasPersonalizacion; }
    public void setNotasPersonalizacion(String notasPersonalizacion) { this.notasPersonalizacion = notasPersonalizacion; }

    // --- Formateo para vista ---
    public String getSubtotalFormateado() {
        return String.format("$ %,.0f", subtotal);
    }

    public String getPrecioUnitarioFormateado() {
        return String.format("$ %,.0f", precioUnitario);
    }

    public boolean esPersonalizado() {
        return anchoPersonalizado != null || altoPersonalizado != null;
    }
}