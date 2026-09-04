package com.smartglass.model.mysql;

import com.smartglass.model.Producto;
import jakarta.persistence.*;

/**
 * NOTA / SUPUESTO: no se pidio explicitamente, pero Pedido.detalles
 * necesita que DetallePedido sea una entidad JPA (con Pedido como
 * dueno de la relacion inversa) para poder aislar la capa relacional
 * por completo. Se asume que Producto ya es una entidad JPA existente
 * en com.smartglass.model; ajusta el import si vive en otro paquete.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

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

    public DetallePedido(Producto producto, int cantidad) {
        this.producto = producto;
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

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

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
