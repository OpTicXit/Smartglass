package com.smartglass.service;

import com.smartglass.model.mongo.Carrito;
import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.repository.mongo.CarritoRepository;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Carrito de compras persistido en MongoDB (antes en Redis via
 * RedisTemplate). Ya no soporta carritos anonimos por sesion HTTP:
 * la entidad Carrito ahora se identifica por usuarioId, en linea con
 * la autenticacion JWT del proyecto. Los metodos de LECTURA
 * (getItems/getTotal/getCantidadTotal/estaVacio) devuelven un
 * carrito vacio si no hay usuario autenticado -- para no romper
 * vistas que muestran el contador del carrito a un visitante -- pero
 * los metodos de ESCRITURA (agregar/actualizar/eliminar/limpiar)
 * exigen sesion autenticada, ya que no hay donde persistirlos si no.
 */
@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarritoService(CarritoRepository carritoRepository, UsuarioRepository usuarioRepository) {
        this.carritoRepository = carritoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Optional<Long> usuarioIdActualSiExiste() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName())
                .map(com.smartglass.model.mysql.Usuario::getId);
    }

    private Long usuarioIdActual() {
        return usuarioIdActualSiExiste()
                .orElseThrow(() -> new IllegalStateException("Se requiere un usuario autenticado para usar el carrito."));
    }

    private Carrito obtenerOCrearCarrito() {
        Long usuarioId = usuarioIdActual();
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> new Carrito(usuarioId));
    }

    public void agregarProducto(CatalogoProducto producto, int cantidad) {
        if (producto == null || cantidad <= 0) return;

        Carrito carrito = obtenerOCrearCarrito();

        Optional<ItemCarrito> itemExistente = carrito.getItems().stream()
                .filter(item -> item.getProducto() != null
                        && producto.getId().equals(item.getProducto().getId()))
                .findFirst();

        if (itemExistente.isPresent()) {
            itemExistente.get().incrementarCantidad(cantidad);
        } else {
            carrito.getItems().add(new ItemCarrito(producto, cantidad));
        }

        carritoRepository.save(carrito);
    }

    public List<ItemCarrito> getItems() {
        return usuarioIdActualSiExiste()
                .flatMap(carritoRepository::findByUsuarioId)
                .map(Carrito::getItems)
                .orElse(Collections.emptyList());
    }

    public void actualizarCantidad(String productoId, int cantidad) {
        if (cantidad <= 0) {
            eliminarProducto(productoId);
            return;
        }

        Carrito carrito = obtenerOCrearCarrito();
        carrito.getItems().stream()
                .filter(item -> item.getProducto() != null
                        && productoId.equals(item.getProducto().getId()))
                .findFirst()
                .ifPresent(item -> item.setCantidad(cantidad));

        carritoRepository.save(carrito);
    }

    public void eliminarProducto(String productoId) {
        Carrito carrito = obtenerOCrearCarrito();
        carrito.getItems().removeIf(item -> item.getProducto() != null
                && productoId.equals(item.getProducto().getId()));
        carritoRepository.save(carrito);
    }

    public double getTotal() {
        return getItems().stream()
                .mapToDouble(ItemCarrito::getSubtotal)
                .sum();
    }

    public int getCantidadTotal() {
        return getItems().stream()
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }

    public void limpiar() {
        Long usuarioId = usuarioIdActual();
        carritoRepository.findByUsuarioId(usuarioId).ifPresent(carritoRepository::delete);
    }

    public boolean estaVacio() {
        return getItems().isEmpty();
    }
}