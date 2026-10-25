package com.smartglass.service;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T2 · stock TRANSACCIONAL (D1): el inventario solo baja con una compra real.
 *
 * reservar() descuenta cada producto del catalogo con un findAndModify
 * condicionado a "stock >= cantidad": dos compras simultaneas nunca dejan el
 * stock negativo. Mongo standalone no tiene transacciones entre documentos, asi
 * que si un producto no alcanza se devuelven las unidades ya descontadas
 * (compensacion) y la compra se rechaza entera.
 *
 * Sin control de inventario (stock null) no se descuenta nada. Las obras a
 * medida (VidrioSostenible) no tienen stock: su id no esta en el catalogo.
 * Lo invoca StockCompraAspect alrededor de PedidoService.crearPedidoDesdeCarrito.
 */
@Service
public class StockService {

    private final MongoTemplate mongoTemplate;
    private final ProductoService productoService;
    private final MessageSource messageSource;
    private final NotificacionService notificacionService;

    public StockService(MongoTemplate mongoTemplate, ProductoService productoService, MessageSource messageSource,
                        NotificacionService notificacionService,
                        @Value("${smartglass.stock.umbral-bajo:5}") int umbralBajo) {
        this.mongoTemplate = mongoTemplate;
        this.productoService = productoService;
        this.messageSource = messageSource;
        this.notificacionService = notificacionService;
        CatalogoProducto.setUmbralStockBajo(umbralBajo);
    }

    /** Stock insuficiente al confirmar: el mensaje llega tal cual al aviso del checkout. */
    public static class StockInsuficienteException extends IllegalStateException {
        public StockInsuficienteException(String mensaje) { super(mensaje); }
    }

    /** productoId -> unidades, sumando lineas repetidas. */
    private static Map<String, Integer> agrupar(List<ItemCarrito> items) {
        Map<String, Integer> porProducto = new LinkedHashMap<>();
        for (ItemCarrito item : items) {
            if (item.getProducto() == null || item.getProducto().getId() == null || item.getCantidad() <= 0) continue;
            porProducto.merge(item.getProducto().getId(), item.getCantidad(), Integer::sum);
        }
        return porProducto;
    }

    /**
     * Descuenta el stock de todo el carrito o de nada. Devuelve lo descontado
     * (para liberar() si el pedido falla despues).
     */
    public Map<String, Integer> reservar(List<ItemCarrito> items) {
        Map<String, Integer> reservado = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : agrupar(items).entrySet()) {
            CatalogoProducto actual = productoService.obtenerPorIdOrNull(e.getKey());
            if (actual == null || !actual.isStockControlado()) continue;   // obra a medida o sin inventario
            Query query = new Query(Criteria.where("_id").is(e.getKey()).and("stock").gte(e.getValue()));
            CatalogoProducto tras = mongoTemplate.findAndModify(query, new Update().inc("stock", -e.getValue()),
                    FindAndModifyOptions.options().returnNew(true), CatalogoProducto.class);
            if (tras == null) {
                liberar(reservado);
                CatalogoProducto ahora = productoService.obtenerPorIdOrNull(e.getKey());
                int quedan = ahora == null || ahora.getStock() == null ? 0 : Math.max(0, ahora.getStock());
                // D2: el numero va como texto para que MessageFormat no lo formatee segun el idioma
                throw new StockInsuficienteException(quedan == 0
                        ? texto("stock.checkout_agotado", actual.getNombre())
                        : texto("stock.checkout_quedan", String.valueOf(quedan), actual.getNombre()));
            }
            reservado.put(e.getKey(), e.getValue());
            // T3: tras la compra real el admin se entera si queda poco o nada
            if (tras.isUltimasUnidades() || tras.isAgotado()) notificacionService.stockBajo(tras);
        }
        return reservado;
    }

    private String texto(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    /** Devuelve unidades reservadas (compensacion de un pedido que no se completo). */
    public void liberar(Map<String, Integer> reservado) {
        if (reservado == null) return;
        for (Map.Entry<String, Integer> e : new ArrayList<>(reservado.entrySet())) {
            mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(e.getKey())),
                    new Update().inc("stock", e.getValue()), CatalogoProducto.class);
        }
    }

    /** Unidades que todavia se pueden anadir al carrito (null = sin limite). */
    public static Integer disponiblesParaAnadir(CatalogoProducto producto, int yaEnCarrito) {
        if (producto == null || !producto.isStockControlado()) return null;
        return Math.max(0, producto.getStock() - Math.max(0, yaEnCarrito));
    }
}
