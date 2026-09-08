package com.smartglass.controller;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.PedidoService;
import com.smartglass.service.ProductoService;
import com.smartglass.service.ReseñaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
    private final ReseñaService resenaService;

    public AdminController(UsuarioRepository usuarioRepository, PedidoService pedidoService,
                            ProductoService productoService, ReseñaService resenaService) {
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

    // --- Catalogo de productos ---

    @GetMapping("/productos")
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.obtenerTodos());
        return "admin/productos";
    }

    @PostMapping("/productos")
    public String crearProducto(@ModelAttribute CatalogoProducto producto, RedirectAttributes ra) {
        productoService.guardar(producto);
        ra.addFlashAttribute("mensaje", "Producto agregado al catálogo.");
        return "redirect:/admin/productos";
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
        model.addAttribute("resenas", resenaService.obtenerTodas());
        return "admin/resenas";
    }

    @PostMapping("/resenas/{id}/eliminar")
    public String eliminarResena(@PathVariable String id, RedirectAttributes ra) {
        resenaService.eliminarPorId(id);
        ra.addFlashAttribute("mensaje", "Reseña eliminada.");
        return "redirect:/admin/resenas";
    }
}