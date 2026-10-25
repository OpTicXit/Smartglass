package com.smartglass.controller;

import com.smartglass.service.Funcion;
import com.smartglass.service.PlanService;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.NotificacionService;
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
import java.time.LocalDate;
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
    private final NotificacionService notificacionService;
    private final PlanService plan;

    public AdminController(UsuarioRepository usuarioRepository, PedidoService pedidoService,
                            ProductoService productoService, ResenaService resenaService,
                            NotificacionService notificacionService, PlanService plan) {
        this.plan = plan;
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.resenaService = resenaService;
        this.notificacionService = notificacionService;
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
    // T6: smartglass.uploads.dir (UPLOADS_DIR); en produccion es un volumen persistente
    @org.springframework.beans.factory.annotation.Value("${smartglass.uploads.dir:uploads}")
    private String carpetaUploads;
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

        // T2: el precio que fija el admin es suyo (el balanceo D3 no lo toca) y el stock no puede ser negativo
        producto.setEsquemaPrecio("manual");
        producto.setPrecioAnterior(null);
        if (producto.getStock() != null && producto.getStock() < 0) producto.setStock(0);
        CatalogoProducto creado = productoService.guardar(producto);
        notificacionService.productoNuevo(creado);              // T3: novedad para todos
        ra.addFlashAttribute("mensaje", "Producto agregado al catálogo.");
        return "redirect:/admin/productos";
    }

    /**
     * T2: stock y oferta de un producto. Parametros explicitos (nunca @ModelAttribute sobre
     * la entidad: evita asignacion masiva). Solo ROLE_ADMIN (regla /admin/** de SecurityConfig).
     * Stock vacio = sin control de inventario.
     */
    @PostMapping("/productos/{id}/comercio")
    public String actualizarComercio(@PathVariable String id,
                                     @RequestParam(required = false) Integer stock,
                                     @RequestParam(required = false) Double precioOferta,
                                     @RequestParam(required = false) String ofertaInicio,
                                     @RequestParam(required = false) String ofertaFin,
                                     @RequestParam(defaultValue = "false") boolean ofertaActiva,
                                     RedirectAttributes ra) {
        CatalogoProducto p = productoService.obtenerPorIdOrNull(id);
        if (p == null) {
            ra.addFlashAttribute("mensaje", "El producto no existe.");
            return "redirect:/admin/productos";
        }
        // T5: sin la funcion OFERTAS_PROGRAMADAS (plan Basico) solo se toca el stock; la oferta ya
        // guardada se conserva tal cual (el formulario la muestra desactivada).
        if (!plan.activa(Funcion.OFERTAS_PROGRAMADAS)) {
            precioOferta = p.getPrecioOferta();
            ofertaInicio = p.getOfertaInicio() != null ? p.getOfertaInicio().toString() : null;
            ofertaFin = p.getOfertaFin() != null ? p.getOfertaFin().toString() : null;
            ofertaActiva = p.isOfertaActiva();
        }
        LocalDate inicio, fin;
        try {
            inicio = ofertaInicio == null || ofertaInicio.isBlank() ? null : LocalDate.parse(ofertaInicio);
            fin = ofertaFin == null || ofertaFin.isBlank() ? null : LocalDate.parse(ofertaFin);
        } catch (java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("mensaje", "Revisa las fechas de la oferta.");
            return "redirect:/admin/productos";
        }
        if (stock != null && stock < 0) {
            ra.addFlashAttribute("mensaje", "El stock no puede ser negativo.");
            return "redirect:/admin/productos";
        }
        if (ofertaActiva && (precioOferta == null || precioOferta <= 0 || precioOferta >= p.getPrecio())) {
            ra.addFlashAttribute("mensaje", "El precio de oferta debe ser mayor que 0 y menor que el precio de lista.");
            return "redirect:/admin/productos";
        }
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            ra.addFlashAttribute("mensaje", "La oferta no puede terminar antes de empezar.");
            return "redirect:/admin/productos";
        }
        boolean ofertaAntes = p.isOfertaVigente();
        Double ofertaPrecioAntes = p.getPrecioOferta();
        boolean agotadoAntes = p.isAgotado();
        p.setStock(stock);
        p.setPrecioOferta(precioOferta);
        p.setOfertaInicio(inicio);
        p.setOfertaFin(fin);
        p.setOfertaActiva(ofertaActiva);
        productoService.guardar(p);
        // T3: novedades para todos solo cuando algo cambia de verdad
        if (p.isOfertaVigente() && (!ofertaAntes || !java.util.Objects.equals(ofertaPrecioAntes, precioOferta))) {
            notificacionService.ofertaNueva(p);
        }
        if (agotadoAntes && p.isStockControlado() && !p.isAgotado()) notificacionService.reposicion(p);
        ra.addFlashAttribute("mensaje", "Stock y oferta de «" + p.getNombre() + "» actualizados.");
        return "redirect:/admin/productos";
    }

    /** Valida y guarda la foto con un nombre generado (nunca el nombre original, para evitar choques y rutas raras). */
    private Path carpetaFotos() { return Paths.get(carpetaUploads, "img"); }

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

        Files.createDirectories(carpetaFotos());
        String nombre = "prod-" + UUID.randomUUID() + extension;
        try (InputStream in = foto.getInputStream()) {
            Files.copy(in, carpetaFotos().resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
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