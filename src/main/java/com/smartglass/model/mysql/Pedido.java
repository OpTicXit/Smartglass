package com.smartglass.model.mysql;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad relacional de Pedido. Reemplaza la version anterior basada
 * en MongoDB (@Document): ahora usa @Entity/@Id de JPA, el id pasa
 * de String (ObjectId) a Long autogenerado, y "usuarioId" se
 * convierte en una relacion @ManyToOne real hacia Usuario.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;

    private double total;

    private String estado;

    @Column(name = "direccion_envio")
    private String direccionEnvio;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "numero_factura", unique = true)
    private String numeroFactura;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @Column(name = "huella_carbono")
    private double huellaCarbono;

    @Column(name = "compensacion_carbono")
    private boolean compensacionCarbono;

    @Column(name = "es_personalizado")
    private boolean esPersonalizado;

    /**
     * Constructor base
     */
    public Pedido() {
        this.fechaPedido = LocalDateTime.now();
        this.estado = "PENDIENTE";
        this.numeroFactura = "TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // --- Logica de negocio ---

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.total = detalles.stream()
                .mapToDouble(DetallePedido::getSubtotal)
                .sum();
    }

    /**
     * Genera el numero de factura oficial una vez que el pedido ya
     * tiene id asignado por la base de datos (antes se basaba en el
     * ObjectId de Mongo; ahora usa el id numerico de MySQL).
     */
    public void generarNumeroFacturaOficial() {
        if (this.id != null) {
            String anio = String.valueOf(LocalDateTime.now().getYear());
            String idFormateado = String.format("%06d", this.id);
            this.numeroFactura = "SG-" + anio + "-" + idFormateado;
        }
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDateTime fechaPedido) { this.fechaPedido = fechaPedido; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }

    public double getHuellaCarbono() { return huellaCarbono; }
    public void setHuellaCarbono(double huellaCarbono) { this.huellaCarbono = huellaCarbono; }

    public boolean isCompensacionCarbono() { return compensacionCarbono; }
    public void setCompensacionCarbono(boolean compensacionCarbono) { this.compensacionCarbono = compensacionCarbono; }

    public boolean isEsPersonalizado() { return esPersonalizado; }
    public void setEsPersonalizado(boolean esPersonalizado) { this.esPersonalizado = esPersonalizado; }

    // --- Formato ---
    public String getTotalFormateado() {
        return String.format("$ %,.0f", total);
    }
}
