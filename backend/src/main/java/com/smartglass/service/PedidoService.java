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

    /** Recargo por compensar la huella del pedido en el checkout (mismo valor que anuncia la vista). */
    public static final double RECARGO_COMPENSACION_PEDIDO = 5000;

    private final PedidoRepository pedidoRepository;
    private final CarritoService carritoService;
    private final AnalisisService analisisService;
    private final VidrioSostenibleService vidrioSostenibleService;
    private final StockService stockService;

    public PedidoService(PedidoRepository pedidoRepository,
                          CarritoService carritoService,
                          AnalisisService analisisService,
                          VidrioSostenibleService vidrioSostenibleService,
                          StockService stockService) {
        this.stockService = stockService;
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

    /**
     * true si todo el carrito son obras a medida que ya pagaron su compensacion en la cotizacion
     * (AnalisisService la suma al precio). En ese caso el checkout no la cobra otra vez.
     */
    public boolean carritoYaCompensado(List<ItemCarrito> items) {
        return !items.isEmpty() && items.stream().allMatch(item -> item.getProducto() != null
                && item.getProducto().getId() != null
                && vidrioSostenibleService.obtenerPorId(item.getProducto().getId())
                        .map(VidrioSostenible::isCompensarCarbono).orElse(false));
    }

    // --- GESTION DE PEDIDOS ---

    public Pedido crearPedidoDesdeCarrito(Usuario usuario, String direccion, String metodo, boolean compensar) {
        return crearPedidoDesdeCarrito(usuario, direccion, metodo, compensar, false, false, false);
    }

    /** T3: igual que el anterior, con los compromisos logisticos sin costo (no tocan total ni huella). */
    public Pedido crearPedidoDesdeCarrito(Usuario usuario, String direccion, String metodo, boolean compensar,
                                          boolean empaqueReutilizable, boolean entregaConsolidada, boolean retomaVidrio) {
        List<ItemCarrito> items = carritoService.getItems();
        if (items.isEmpty()) throw new IllegalStateException("Carrito vacío");
        // Red de seguridad: una cantidad o un precio corruptos nunca deben dar un pedido negativo
        if (items.stream().anyMatch(i -> i.getCantidad() <= 0 || !(i.getSubtotal() >= 0)))
            throw new IllegalStateException("Carrito con cantidades o precios no validos");

        double subtotal = items.stream().mapToDouble(ItemCarrito::getSubtotal).sum();

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodo);
        boolean yaCompensado = carritoYaCompensado(items);
        pedido.setCompensacionCarbono(compensar || yaCompensado);
        pedido.setEmpaqueReutilizable(empaqueReutilizable);
        pedido.setEntregaConsolidada(entregaConsolidada);
        pedido.setRetomaVidrio(retomaVidrio);

        items.forEach(item -> pedido.agregarDetalle(new DetallePedido(item.getProducto(), item.getCantidad())));

        pedido.recalcularTotales();

        pedido.setTotal(pedido.getTotal() + calcularCostoEnvio(subtotal));
        // La compensacion que anuncia el checkout ("+$5.000") se cobra en el pedido y queda desglosada
        if (compensar && !yaCompensado) {
            pedido.setRecargoCompensacion(RECARGO_COMPENSACION_PEDIDO);
            pedido.setTotal(pedido.getTotal() + RECARGO_COMPENSACION_PEDIDO);
        }
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

        // Un pedido cancelado no se fabrica ni se cobra: no suma ingresos ni CO2 (igual que FinanzasService)
        List<Pedido> validos = pedidos.stream().filter(p -> !CANCELADO.equals(p.getEstado())).toList();
        double ingresos = validos.stream().mapToDouble(Pedido::getTotal).sum();
        double co2 = validos.stream().mapToDouble(Pedido::getHuellaCarbono).sum();
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

    private static final String CANCELADO = "CANCELADO";

    /** Lo que paso al guardar varios estados: cuantos cambiaron y que facturas no se pudieron reactivar. */
    public record CambioEstados(int actualizados, List<String> sinStock) {}

    @Transactional
    public CambioEstados actualizarEstado(Long id, String estado) {
        return actualizarEstados(Map.of(id, estado));
    }

    /**
     * Cambia el estado de varios pedidos en una sola transaccion (id -> nuevo estado).
     * Solo toca los pedidos cuyo estado realmente cambia.
     * El inventario acompana: cancelar devuelve las unidades del pedido y reactivar un cancelado las vuelve
     * a apartar; si ya no hay suficientes, ese pedido se queda cancelado y su factura sale en sinStock.
     */
    @Transactional
    public CambioEstados actualizarEstados(Map<Long, String> cambios) {
        int actualizados = 0;
        List<String> sinStock = new java.util.ArrayList<>();
        for (Map.Entry<Long, String> cambio : cambios.entrySet()) {
            Optional<Pedido> encontrado = pedidoRepository.findById(cambio.getKey());
            if (encontrado.isEmpty() || cambio.getValue().equals(encontrado.get().getEstado())) continue;
            Pedido pedido = encontrado.get();
            Map<String, Integer> unidades = new java.util.LinkedHashMap<>();
            pedido.getDetalles().forEach(d -> { if (d.getProductoId() != null && d.getCantidad() > 0) unidades.merge(d.getProductoId(), d.getCantidad(), Integer::sum); });
            if (CANCELADO.equals(pedido.getEstado())) {
                try {
                    stockService.reservar(unidades);
                } catch (StockService.StockInsuficienteException e) {
                    sinStock.add(pedido.getNumeroFactura());
                    continue;
                }
            } else if (CANCELADO.equals(cambio.getValue())) {
                stockService.liberar(unidades);
            }
            pedido.setEstado(cambio.getValue());
            pedidoRepository.save(pedido);
            actualizados++;
        }
        return new CambioEstados(actualizados, sinStock);
    }
}