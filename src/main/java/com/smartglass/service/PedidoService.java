package com.smartglass.service;

import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mysql.DetallePedido;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NOTA: se actualizaron los imports de Pedido/DetallePedido/Usuario
 * a com.smartglass.model.mysql (movidos en un paso anterior de este
 * refactor) y el de PedidoRepository a com.smartglass.repository.mysql
 * (Pedido ahora es una entidad JPA, no un documento Mongo).
 * El calculo de huella de carbono ya NO vive aqui: se delega en
 * AnalisisService para que exista una sola fuente de verdad.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarritoService carritoService;
    private final AnalisisService analisisService;

    public PedidoService(PedidoRepository pedidoRepository,
                          CarritoService carritoService,
                          AnalisisService analisisService) {
        this.pedidoRepository = pedidoRepository;
        this.carritoService = carritoService;
        this.analisisService = analisisService;
    }

    // --- METODOS DE CALCULO PARA EL CHECKOUT ---

    /**
     * Calcula el costo de envio basado en el subtotal.
     * Si la compra supera los $500,000, el envio es gratis.
     */
    public double calcularCostoEnvio(double subtotal) {
        return (subtotal > 500000) ? 0.0 : 25000.0;
    }

    /**
     * Calcula la huella de carbono total de los items en el carrito,
     * delegando el calculo por item en AnalisisService.
     */
    public double calcularHuellaCarbono(List<ItemCarrito> items) {
        double totalCO2 = items.stream().mapToDouble(item -> {
            var producto = item.getProducto();
            return analisisService.calcularHuellaCarbonoItem(
                    producto.getAncho(),
                    producto.getAlto(),
                    producto.getEspesor(),
                    producto.getTipo(),
                    item.getCantidad()
            );
        }).sum();

        return Math.round(totalCO2 * 100.0) / 100.0;
    }

    // --- GESTION DE PEDIDOS ---

    public Pedido crearPedidoDesdeCarrito(Usuario usuario, String direccion, String metodo, boolean compensar) {
        List<ItemCarrito> items = carritoService.getItems();
        if (items.isEmpty()) throw new IllegalStateException("Carrito vacío");

        double subtotal = items.stream().mapToDouble(ItemCarrito::getSubtotal).sum();

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodo);
        pedido.setCompensacionCarbono(compensar);

        items.forEach(item -> pedido.agregarDetalle(new DetallePedido(item.getProducto(), item.getCantidad())));

        pedido.recalcularTotales();

        pedido.setTotal(pedido.getTotal() + calcularCostoEnvio(subtotal));
        pedido.setHuellaCarbono(calcularHuellaCarbono(items));

        Pedido guardado = pedidoRepository.save(pedido);
        carritoService.limpiar();
        return guardado;
    }

    public Map<String, Object> obtenerEstadisticasDashboard() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        double ingresos = pedidos.stream().mapToDouble(Pedido::getTotal).sum();
        double co2 = pedidos.stream().mapToDouble(Pedido::getHuellaCarbono).sum();
        long pendientes = pedidos.stream()
                .filter(p -> "PENDIENTE".equals(p.getEstado()) || "PROCESANDO".equals(p.getEstado()))
                .count();

        stats.put("ingresos", ingresos);
        stats.put("co2Total", co2);
        stats.put("pendientes", pendientes);
        stats.put("totalPedidos", pedidos.size());
        stats.put("pedidos", pedidos);

        return stats;
    }

    public List<Pedido> obtenerTodos() { return pedidoRepository.findAll(); }

    public Optional<Pedido> obtenerPorId(Long id) { return pedidoRepository.findById(id); }

    public List<Pedido> obtenerPedidosUsuario(Usuario usuario) {
        return pedidoRepository.findByUsuarioIdOrderByFechaPedidoDesc(usuario.getId());
    }

    public void actualizarEstado(Long id, String estado) {
        pedidoRepository.findById(id).ifPresent(p -> {
            p.setEstado(estado);
            pedidoRepository.save(p);
        });
    }
}