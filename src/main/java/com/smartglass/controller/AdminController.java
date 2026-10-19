package com.smartglass.controller;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.PedidoService;
import com.smartglass.service.ProductoService;
import com.smartglass.service.ResenaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Panel de administracion. Ya no es solo lectura: puede cambiar el
 * estado de un pedido, el rol de un usuario, crear/eliminar
 * productos del catalogo, y moderar reseñas -- todo contra la base
 * de datos real, sin datos de ejemplo ni cache: cada GET vuelve a
 * consultar PedidoService/UsuarioRepository/ProductoService/
 * ResenaService, asi que siempre refleja el estado actual del
 * sistema.
 *
 * SecurityConfig ya protege "/admin/**" con hasRole("ADMIN"), asi
 * que no hace falta repetir esa comprobacion aqui.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final ResenaService resenaService;

    public AdminController(UsuarioRepository usuarioRepository, PedidoService pedidoService,
                            ProductoService productoService, ResenaService resenaService) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.resenaService = resenaService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", pedidoService.obtenerEstadisticasDashboard());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalProductos", productoService.obtenerTodos().size());
        return "admin/dashboard";
    }

    // --- Usuarios ---

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/{id}")
    public String verUsuario(@PathVariable Long id, Model model) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    model.addAttribute("usuario", usuario);
                    return "admin/usuario-detalle";
                })
                .orElse("redirect:/admin/usuarios");
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam String tipoUsuario, RedirectAttributes ra) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setTipoUsuario(tipoUsuario);
            usuarioRepository.save(usuario);
        });
        ra.addFlashAttribute("mensaje", "Rol actualizado.");
        return "redirect:/admin/usuarios/" + id;
    }

    // --- Pedidos ---

    @GetMapping("/pedidos")
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", pedidoService.obtenerTodos());
        return "admin/pedidos";
    }

    @PostMapping("/pedidos/{id}/estado")
    public String cambiarEstadoPedido(@PathVariable Long id, @RequestParam String estado, RedirectAttributes ra) {
        pedidoService.actualizarEstado(id, estado);
        ra.addFlashAttribute("mensaje", "Estado del pedido actualizado.");
        return "redirect:/admin/pedidos";
    }

    private static final Set<String> ESTADOS_PEDIDO =
            Set.of("PENDIENTE", "PROCESANDO", "ENVIADO", "ENTREGADO", "CANCELADO");

    /**
     * Guarda de una vez los cambios de estado de varios pedidos.
     * La vista envia un campo por pedido con el nombre "estado_{id}" (por ejemplo estado_12=ENVIADO).
     */
    @PostMapping("/pedidos/estados")
    public String cambiarEstadosPedidos(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        Map<Long, String> cambios = new LinkedHashMap<>();
        for (Map.Entry<String, String> campo : params.entrySet()) {
            if (!campo.getKey().startsWith("estado_")) continue;
            String estado = campo.getValue();
            if (!ESTADOS_PEDIDO.contains(estado)) continue;
            try {
                cambios.put(Long.parseLong(campo.getKey().substring("estado_".length())), estado);
            } catch (NumberFormatException ignorado) {
                // campo con id invalido: se ignora
            }
        }

        int actualizados = pedidoService.actualizarEstados(cambios);
        ra.addFlashAttribute("mensaje",
                actualizados == 0 ? "No había cambios que guardar."
                : actualizados == 1 ? "Estado del pedido actualizado."
                : actualizados + " pedidos actualizados.");
        return "redirect:/admin/pedidos";
    }

    // --- Catalogo de productos ---

    @GetMapping("/productos")
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.obtenerTodos());
        return "admin/productos";
    }

    /** Carpeta donde se guardan las fotos subidas (la sirve UploadsWebConfig bajo /img/**). */
    private static final Path CARPETA_FOTOS = Paths.get("uploads", "img");
    private static final long MAX_BYTES_FOTO = 5L * 1024 * 1024;

    /**
     * Crea un producto. La foto es opcional:
     *  - si se sube un archivo ("foto"), se guarda en disco y su nombre queda en "imagen";
     *  - si no, se usa el nombre de archivo escrito en "imagen" (uno que ya este en /img/);
     *  - si no hay ninguno, el producto se crea sin foto (imagen = null) y la vista muestra "Sin foto".
     */
    @PostMapping("/productos")
    public String crearProducto(@ModelAttribute CatalogoProducto producto,
                                @RequestParam(value = "foto", required = false) MultipartFile foto,
                                RedirectAttributes ra) {
        try {
            if (foto != null && !foto.isEmpty()) {
                producto.setImagen(guardarFoto(foto));
            } else {
                String imagen = producto.getImagen();
                producto.setImagen(imagen == null || imagen.isBlank() ? null : imagen.trim());
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            return "redirect:/admin/productos";
        } catch (IOException e) {
            ra.addFlashAttribute("mensaje", "No se pudo guardar la foto. Intenta de nuevo.");
            return "redirect:/admin/productos";
        }

        productoService.guardar(producto);
        ra.addFlashAttribute("mensaje", "Producto agregado al catálogo.");
        return "redirect:/admin/productos";
    }

    /** Valida y guarda la foto con un nombre generado (nunca el nombre original, para evitar choques y rutas raras). */
    private String guardarFoto(MultipartFile foto) throws IOException {
        if (foto.getSize() > MAX_BYTES_FOTO) {
            throw new IllegalArgumentException("La foto pesa más de 5 MB.");
        }
        String tipo = foto.getContentType();
        String extension;
        if ("image/png".equals(tipo)) extension = ".png";
        else if ("image/jpeg".equals(tipo)) extension = ".jpg";
        else if ("image/webp".equals(tipo)) extension = ".webp";
        else if ("image/gif".equals(tipo)) extension = ".gif";
        else throw new IllegalArgumentException("La foto debe ser PNG, JPG, WEBP o GIF.");

        Files.createDirectories(CARPETA_FOTOS);
        String nombre = "prod-" + UUID.randomUUID() + extension;
        try (InputStream in = foto.getInputStream()) {
            Files.copy(in, CARPETA_FOTOS.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        }
        return nombre;
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminarProducto(@PathVariable String id, RedirectAttributes ra) {
        productoService.eliminarPorId(id);
        ra.addFlashAttribute("mensaje", "Producto eliminado.");
        return "redirect:/admin/productos";
    }

    // --- Moderacion de resenas ---

    @GetMapping("/resenas")
    public String listarResenas(Model model) {
        // productoId -> nombre, para que la vista agrupe las reseñas por
        // producto y muestre su nombre (la reseña solo guarda el id).
        Map<String, String> nombresProducto = new HashMap<>();
        for (CatalogoProducto p : productoService.obtenerTodos()) {
            if (p.getId() != null) {
                nombresProducto.put(String.valueOf(p.getId()), p.getNombre());
            }
        }

        model.addAttribute("resenas", resenaService.obtenerTodas());
        model.addAttribute("nombresProducto", nombresProducto);
        return "admin/resenas";
    }

    @PostMapping("/resenas/{id}/eliminar")
    public String eliminarResena(@PathVariable String id, RedirectAttributes ra) {
        resenaService.eliminarPorId(id);
        ra.addFlashAttribute("mensaje", "Reseña eliminada.");
        return "redirect:/admin/resenas";
    }
}