package com.smartglass.service;

import com.smartglass.model.mongo.Carrito;
import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mongo.ItemCarrito;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.CarritoRepository;
import com.smartglass.repository.mysql.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carrito de compras.
 * - Usuario autenticado: persistido en MongoDB, identificado por usuarioId.
 * - T1 (D4) anonimo: vive en la sesion HTTP como productoId -> cantidad (solo
 *   productos del catalogo; precio y datos se leen del catalogo al mostrarlo).
 *   No toca Mongo ni otra sesion; se pierde al cerrar la sesion.
 * - Al autenticarse, fusionarCarritoAnonimo() lo suma al carrito del usuario
 *   (lo llama CustomSuccessHandler, en formLogin y en Google).
 */
@Service
public class CarritoService {

    /** Atributo de sesion del carrito anonimo: LinkedHashMap<String, Integer>. */
    public static final String SESION_CARRITO_ANONIMO = "carritoAnonimo";

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoService productoService;

    public CarritoService(CarritoRepository carritoRepository, UsuarioRepository usuarioRepository,
                          ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoService = productoService;
    }

    private static boolean esAnonimo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal());
    }

    private Optional<Long> usuarioIdActualSiExiste() {
        if (esAnonimo()) return Optional.empty();
        return usuarioRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .map(com.smartglass.model.mysql.Usuario::getId);
    }

    private Long usuarioIdActual() {
        return usuarioIdActualSiExiste()
                .orElseThrow(() -> new IllegalStateException("Se requiere un usuario autenticado para usar el carrito."));
    }

    private Carrito obtenerOCrearCarrito(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> new Carrito(usuarioId));
    }

    // ------------------------------------------------------------ carrito anonimo

    private static HttpSession sesionActual(boolean crear) {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (!(ra instanceof ServletRequestAttributes sra)) return null;
        return sra.getRequest().getSession(crear);
    }

    /** Carrito anonimo de la sesion; null si no hay (y crear es false). */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> carritoAnonimo(HttpSession sesion, boolean crear) {
        if (sesion == null) return null;
        Object valor = sesion.getAttribute(SESION_CARRITO_ANONIMO);
        if (valor instanceof Map<?, ?> m) return (Map<String, Integer>) m;
        if (!crear) return null;
        Map<String, Integer> nuevo = new LinkedHashMap<>();
        sesion.setAttribute(SESION_CARRITO_ANONIMO, nuevo);
        return nuevo;
    }

    /** Reescribe el atributo para que el contenedor lo marque como modificado. */
    private static void guardarAnonimo(HttpSession sesion, Map<String, Integer> carrito) {
        if (carrito.isEmpty()) sesion.removeAttribute(SESION_CARRITO_ANONIMO);
        else sesion.setAttribute(SESION_CARRITO_ANONIMO, carrito);
    }

    private List<ItemCarrito> itemsAnonimos() {
        Map<String, Integer> carrito = carritoAnonimo(sesionActual(false), false);
        if (carrito == null || carrito.isEmpty()) return Collections.emptyList();
        List<ItemCarrito> items = new ArrayList<>();
        carrito.forEach((productoId, cantidad) -> {
            CatalogoProducto producto = productoService.obtenerPorIdOrNull(productoId);
            if (producto != null && cantidad != null && cantidad > 0) items.add(new ItemCarrito(producto.copiaParaCarrito(), cantidad));
        });
        return items;
    }

    /**
     * Suma el carrito anonimo de la sesion al carrito del usuario y lo borra de
     * la sesion. Un producto que ya estaba en su carrito suma cantidades; nada
     * de lo que el usuario tenia se pierde. Devuelve las unidades transferidas.
     */
    public int fusionarCarritoAnonimo(HttpSession sesion, Long usuarioId) {
        Map<String, Integer> anonimo = carritoAnonimo(sesion, false);
        if (anonimo == null || anonimo.isEmpty() || usuarioId == null) return 0;
        Carrito carrito = obtenerOCrearCarrito(usuarioId);
        int unidades = 0;
        for (Map.Entry<String, Integer> e : anonimo.entrySet()) {
            CatalogoProducto producto = productoService.obtenerPorIdOrNull(e.getKey());
            if (producto == null || e.getValue() == null || e.getValue() <= 0) continue;
            sumar(carrito, producto, e.getValue());
            unidades += e.getValue();
        }
        carritoRepository.save(carrito);
        sesion.removeAttribute(SESION_CARRITO_ANONIMO);
        return unidades;
    }

    // ------------------------------------------------------------------ comunes

    /** T2: una linea nueva guarda el precio vigente (oferta incluida); una existente conserva el suyo. */
    private static void sumar(Carrito carrito, CatalogoProducto producto, int cantidad) {
        Optional<ItemCarrito> itemExistente = carrito.getItems().stream()
                .filter(item -> item.getProducto() != null
                        && producto.getId().equals(item.getProducto().getId()))
                .findFirst();
        if (itemExistente.isPresent()) {
            itemExistente.get().incrementarCantidad(cantidad);
        } else {
            carrito.getItems().add(new ItemCarrito(producto.copiaParaCarrito(), cantidad));
        }
    }

    public void agregarProducto(CatalogoProducto producto, int cantidad) {
        if (producto == null || cantidad <= 0) return;
        if (esAnonimo()) {
            HttpSession sesion = sesionActual(true);
            if (sesion == null) return;
            Map<String, Integer> carrito = carritoAnonimo(sesion, true);
            carrito.merge(producto.getId(), cantidad, Integer::sum);
            guardarAnonimo(sesion, carrito);
            return;
        }
        Carrito carrito = obtenerOCrearCarrito(usuarioIdActual());
        sumar(carrito, producto, cantidad);
        carritoRepository.save(carrito);
    }

    /**
     * PARCHES 3: el dueno de una obra APROBADA la anade a su carrito.
     * Se construye un producto "snapshot" embebido en el item (NO se
     * guarda en el catalogo) para que total y CO2 del checkout
     * coincidan con lo que la cotizacion mostro.
     * T1: exige sesion (una obra aprobada siempre tiene dueno); nunca va al carrito anonimo.
     */
    public void agregarCotizacionAprobada(VidrioSostenible vidrio) {
        if (vidrio == null) return;
        CatalogoProducto snapshot = new CatalogoProducto();
        snapshot.setId(vidrio.getId());
        snapshot.setNombre(vidrio.getTipoVidrio() + " · obra a medida");
        snapshot.setTipo(vidrio.getTipoVidrio());
        snapshot.setPrecio(vidrio.getPrecioEstimado());
        snapshot.setAncho(vidrio.getAncho());
        snapshot.setAlto(vidrio.getAlto());
        snapshot.setEspesor(vidrio.getEspesor());
        Carrito carrito = obtenerOCrearCarrito(usuarioIdActual());
        sumar(carrito, snapshot, 1);
        carritoRepository.save(carrito);
    }

    public List<ItemCarrito> getItems() {
        if (esAnonimo()) return itemsAnonimos();
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
        if (esAnonimo()) {
            HttpSession sesion = sesionActual(false);
            Map<String, Integer> carrito = carritoAnonimo(sesion, false);
            if (carrito == null || !carrito.containsKey(productoId)) return;
            carrito.put(productoId, cantidad);
            guardarAnonimo(sesion, carrito);
            return;
        }
        Carrito carrito = obtenerOCrearCarrito(usuarioIdActual());
        carrito.getItems().stream()
                .filter(item -> item.getProducto() != null
                        && productoId.equals(item.getProducto().getId()))
                .findFirst()
                .ifPresent(item -> item.setCantidad(cantidad));
        carritoRepository.save(carrito);
    }

    public void eliminarProducto(String productoId) {
        if (esAnonimo()) {
            HttpSession sesion = sesionActual(false);
            Map<String, Integer> carrito = carritoAnonimo(sesion, false);
            if (carrito == null) return;
            carrito.remove(productoId);
            guardarAnonimo(sesion, carrito);
            return;
        }
        Carrito carrito = obtenerOCrearCarrito(usuarioIdActual());
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
        if (esAnonimo()) {
            HttpSession sesion = sesionActual(false);
            if (sesion != null) sesion.removeAttribute(SESION_CARRITO_ANONIMO);
            return;
        }
        carritoRepository.findByUsuarioId(usuarioIdActual()).ifPresent(carritoRepository::delete);
    }

    public boolean estaVacio() {
        return getItems().isEmpty();
    }
}
