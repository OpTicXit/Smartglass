package com.smartglass.service;

import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.DetallePedido;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NOTA: se actualizaron los imports de Pedido/DetallePedido/Usuario
 * a com.smartglass.model.mysql y el de PedidoRepository a
 * com.smartglass.repository.mysql (Pedido es una entidad JPA).
 * El calculo de huella por item de CATALOGO vive en AnalisisService.
 *
 * F12: las obras a medida aprobadas entran al carrito como snapshot cuyo
 * id es el de la cotizacion (VidrioSostenible). Para esos items la huella
 * es la ALMACENADA en la cotizacion (x cantidad), no un recalculo con los
 * factores de catalogo: asi cotizacion, checkout y factura muestran el
 * mismo CO2. Los items de catalogo siguen usando AnalisisService.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarritoService carritoService;
    private final AnalisisService analisisService;
    private final VidrioSostenibleService vidrioSostenibleService;

    public PedidoService(PedidoRepository pedidoRepository,
                          CarritoService carritoService,
                          AnalisisService analisisService,
                          VidrioSostenibleService vidrioSostenibleService) {
        this.pedidoRepository = pedidoRepository;
        this.carritoService = carritoService;
        this.analisisService = analisisService;
        this.vidrioSostenibleService = vidrioSostenibleService;
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
     * Calcula la huella de carbono total de los items en el carrito.
     * Obra a medida (id de una cotizacion existente) -> huella almacenada x cantidad.
     * Producto de catalogo -> AnalisisService.calcularHuellaCarbonoItem.
     */
    public double calcularHuellaCarbono(List<ItemCarrito> items) {
        double totalCO2 = items.stream().mapToDouble(item -> {
            var producto = item.getProducto();
            Optional<VidrioSostenible> obra = producto.getId() == null
                    ? Optional.empty()
                    : vidrioSostenibleService.obtenerPorId(producto.getId());
            if (obra.isPresent()) {
                return obra.get().getHuellaCarbono() * item.getCantidad();
            }
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
        // F29: con el id ya asignado por MySQL se genera el numero oficial SG-AAAA-000000
        // (antes la factura se quedaba con el provisional TMP-XXXXXXXX).
        guardado.generarNumeroFacturaOficial();
        guardado = pedidoRepository.save(guardado);
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

    /**
     * Cambia el estado de varios pedidos en una sola transaccion (id -> nuevo estado).
     * Solo toca los pedidos cuyo estado realmente cambia.
     * Devuelve cuantos pedidos se actualizaron.
     */
    @Transactional
    public int actualizarEstados(Map<Long, String> cambios) {
        int actualizados = 0;
        for (Map.Entry<Long, String> cambio : cambios.entrySet()) {
            Optional<Pedido> encontrado = pedidoRepository.findById(cambio.getKey());
            if (encontrado.isPresent() && !cambio.getValue().equals(encontrado.get().getEstado())) {
                Pedido pedido = encontrado.get();
                pedido.setEstado(cambio.getValue());
                pedidoRepository.save(pedido);
                actualizados++;
            }
        }
        return actualizados;
    }
}