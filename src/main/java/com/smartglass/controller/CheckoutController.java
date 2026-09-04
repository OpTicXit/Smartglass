package com.smartglass.controller;

import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.CarritoService;
import com.smartglass.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * AJUSTES:
 * - Pedido/Usuario ahora vienen de com.smartglass.model.mysql.
 * - El id de Pedido es Long (autogenerado por MySQL), ya no String
 *   (ObjectId de Mongo): se cambiaron los @PathVariable de
 *   "confirmacion/{id}" y "pedido/{id}" de String a Long.
 * - Pedido ya no tiene getUsuarioId(): ahora expone getUsuario()
 *   (relacion @ManyToOne). El chequeo de propiedad en verPedido()
 *   se actualizo para comparar p.getUsuario().getId() directamente
 *   (ya no hace falta el hack de .toString(), ambos son Long).
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CarritoService carritoService;
    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;

    public CheckoutController(CarritoService carritoService, PedidoService pedidoService, UsuarioRepository usuarioRepository) {
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrarCheckout(Principal principal, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";
        if (carritoService.estaVacio()) return "redirect:/carrito";

        double subtotal = carritoService.getTotal();
        double costoEnvio = pedidoService.calcularCostoEnvio(subtotal);
        double huellaCarbono = pedidoService.calcularHuellaCarbono(carritoService.getItems());

        model.addAttribute("usuario", usuario);
        model.addAttribute("items", carritoService.getItems());
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("costoEnvio", costoEnvio);
        model.addAttribute("huellaCarbono", huellaCarbono);
        model.addAttribute("total", subtotal + costoEnvio);

        return "checkout";
    }

    @PostMapping("/procesar")
    public String procesarPedido(@RequestParam String direccionEnvio,
        @RequestParam String ciudad,
        @RequestParam String metodoPago,
        @RequestParam(defaultValue = "false") boolean compensarCarbono,
        Principal principal, HttpSession session, RedirectAttributes ra) {

        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        try {
            String direccionCompleta = direccionEnvio + ", " + ciudad;
            Pedido pedido = pedidoService.crearPedidoDesdeCarrito(usuario, direccionCompleta, metodoPago, compensarCarbono);

            session.setAttribute("ultimoPedidoId", pedido.getId());
            return "redirect:/checkout/confirmacion/" + pedido.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al procesar el pedido: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "error");
            return "redirect:/checkout";
        }
    }

    @GetMapping("/confirmacion/{id}")
    public String verConfirmacion(@PathVariable Long id, Principal principal, Model model) {
        return pedidoService.obtenerPorId(id).map(p -> {
            model.addAttribute("pedido", p);
            return "pedido-confirmacion";
        }).orElse("redirect:/usuario");
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(Principal principal, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";
        model.addAttribute("pedidos", pedidoService.obtenerPedidosUsuario(usuario));
        model.addAttribute("usuario", usuario);
        return "mis-pedidos";
    }

    @GetMapping("/pedido/{id}")
    public String verPedido(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        return pedidoService.obtenerPorId(id)
                .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId()))
                .map(p -> {
                    model.addAttribute("pedido", p);
                    model.addAttribute("usuario", usuario);
                    return "factura";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("mensaje", "No tienes permisos para ver este documento.");
                    ra.addFlashAttribute("tipoMensaje", "error");
                    return "redirect:/checkout/mis-pedidos";
                });
    }
}