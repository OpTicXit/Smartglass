package com.smartglass.controller;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Reemplaza al antiguo UsuarioController. El login/registro ahora
 * vive en AuthController (API REST con JWT), asi que este
 * controller queda enfocado exclusivamente en gestion interna
 * (panel de administracion): listar usuarios, ver su detalle y
 * revisar pedidos/estadisticas.
 *
 * SecurityConfig ya protege "/admin/**" con hasRole("ADMIN"), asi
 * que no hace falta repetir esa comprobacion aqui.
 *
 * SUPUESTO: se eliminaron los endpoints de vitrina (/Tienda,
 * /TiendaDestacada, /info/{id}) y el @ModelAttribute que resolvia
 * "usuario logueado" via Principal/OAuth2 -- eso pertenecia a
 * paginas publicas para cualquier visitante, no a gestion interna.
 * Si todavia necesitas esas paginas, lo mas limpio es un
 * TiendaController aparte (puedo generarlo si lo confirmas).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;

    public AdminController(UsuarioRepository usuarioRepository, PedidoService pedidoService) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", pedidoService.obtenerEstadisticasDashboard());
        return "admin/dashboard";
    }

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

    @GetMapping("/pedidos")
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", pedidoService.obtenerTodos());
        return "admin/pedidos";
    }
}