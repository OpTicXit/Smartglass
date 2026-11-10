package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.DetallePedido;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El inventario acompana al estado del pedido y la compensacion de CO2 no se cobra dos veces. */
class PedidoServiceTest {

    private final PedidoRepository repo = mock(PedidoRepository.class);
    private final CarritoService carrito = mock(CarritoService.class);
    private final VidrioSostenibleService vidrios = mock(VidrioSostenibleService.class);
    private final StockService stock = mock(StockService.class);
    private PedidoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PedidoService(repo, carrito, new AnalisisService(), vidrios, stock);
        when(repo.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static CatalogoProducto producto(String id, double precio) {
        CatalogoProducto p = new CatalogoProducto();
        p.setId(id);
        p.setNombre("Vidrio " + id);
        p.setPrecio(precio);
        return p;
    }

    private Pedido pedidoGuardado(String estado) {
        Pedido p = new Pedido();
        p.setEstado(estado);
        p.agregarDetalle(new DetallePedido(producto("p1", 100_000), 2));
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void cancelarDevuelveLasUnidadesAlInventario() {
        Pedido p = pedidoGuardado("PENDIENTE");

        PedidoService.CambioEstados r = servicio.actualizarEstados(Map.of(1L, "CANCELADO"));

        assertEquals(1, r.actualizados());
        assertEquals("CANCELADO", p.getEstado());
        verify(stock).liberar(Map.of("p1", 2));
        verify(stock, never()).reservar(anyMap());
    }

    @Test
    void reactivarUnCanceladoVuelveAApartarYSiNoHayExistenciasSigueCancelado() {
        Pedido p = pedidoGuardado("CANCELADO");
        when(stock.reservar(anyMap())).thenThrow(new StockService.StockInsuficienteException("no alcanza"));

        PedidoService.CambioEstados r = servicio.actualizarEstados(Map.of(1L, "PROCESANDO"));

        assertEquals(0, r.actualizados());
        assertEquals(List.of(p.getNumeroFactura()), r.sinStock());
        assertEquals("CANCELADO", p.getEstado(), "sin existencias el pedido no se reactiva");
    }

    @Test
    void pasarDePendienteAEnviadoNoTocaElInventario() {
        pedidoGuardado("PENDIENTE");

        servicio.actualizarEstados(Map.of(1L, "ENVIADO"));

        verify(stock, never()).liberar(anyMap());
        verify(stock, never()).reservar(anyMap());
    }

    @Test
    void unaObraQueYaPagoSuCompensacionNoLaPagaOtraVezEnElCheckout() {
        VidrioSostenible obra = new VidrioSostenible();
        obra.setCompensarCarbono(true);
        obra.setHuellaCarbono(19.1);
        when(vidrios.obtenerPorId("obra1")).thenReturn(Optional.of(obra));
        when(carrito.getItems()).thenReturn(List.of(new ItemCarrito(producto("obra1", 180_709), 1)));

        Pedido pedido = servicio.crearPedidoDesdeCarrito(new Usuario("Ana", "ana@example.com", "ana", "x"), "Calle 1", "PSE", true);

        assertTrue(pedido.isCompensacionCarbono(), "el pedido cuenta como compensado");
        assertEquals(0, pedido.getRecargoCompensacion(), "sin segundo recargo de $5.000");
        assertEquals(180_709 + 25_000, pedido.getTotal(), "producto + envio, nada mas");
    }

    @Test
    void unProductoDeCatalogoSiPagaLaCompensacionSiSeMarca() {
        when(vidrios.obtenerPorId("p1")).thenReturn(Optional.empty());
        when(carrito.getItems()).thenReturn(List.of(new ItemCarrito(producto("p1", 100_000), 1)));

        Pedido pedido = servicio.crearPedidoDesdeCarrito(new Usuario("Ana", "ana@example.com", "ana", "x"), "Calle 1", "PSE", true);

        assertEquals(PedidoService.RECARGO_COMPENSACION_PEDIDO, pedido.getRecargoCompensacion());
        assertEquals(100_000 + 25_000 + 5_000, pedido.getTotal());
    }
}
