package com.smartglass.controller;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.CarritoService;
import com.smartglass.service.PedidoService;
import com.smartglass.service.VidrioSostenibleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CarritoService carritoService;
    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final VidrioSostenibleService vidrioSostenibleService;

    public CheckoutController(CarritoService carritoService, PedidoService pedidoService, UsuarioRepository usuarioRepository,
                              VidrioSostenibleService vidrioSostenibleService) {
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
        this.vidrioSostenibleService = vidrioSostenibleService;
    }

    /** T3: obras a medida del carrito (el id del item es el de la cotizacion); los productos de catalogo no cuentan. */
    private List<VidrioSostenible> obrasDelCarrito() {
        return carritoService.getItems().stream()
                .map(i -> i.getProducto() == null || i.getProducto().getId() == null ? null : i.getProducto().getId())
                .filter(java.util.Objects::nonNull)
                .map(id -> vidrioSostenibleService.obtenerPorId(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
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
        model.addAttribute("recargoCompensacion", PedidoService.RECARGO_COMPENSACION_PEDIDO);
        // T3: lo que el cliente ya eligio al cotizar sus obras llega marcado al pagar
        List<VidrioSostenible> obras = obrasDelCarrito();
        model.addAttribute("preEmpaque", obras.stream().anyMatch(VidrioSostenible::isEmpaqueReutilizable));
        model.addAttribute("preConsolidada", obras.stream().anyMatch(VidrioSostenible::isEntregaConsolidada));
        model.addAttribute("preRetoma", obras.stream().anyMatch(VidrioSostenible::isRetomaVidrio));
        model.addAttribute("hayUrgente", obras.stream().anyMatch(VidrioSostenible::isUrgente));
        return "checkout";
    }

    @PostMapping("/procesar")
    public String procesarPedido(@RequestParam String direccionEnvio,
                                 @RequestParam String ciudad,
                                 @RequestParam String metodoPago,
                                 @RequestParam(defaultValue = "false") boolean compensarCarbono,
                                 @RequestParam(defaultValue = "false") boolean empaqueReutilizable,
                                 @RequestParam(defaultValue = "false") boolean entregaConsolidada,
                                 @RequestParam(defaultValue = "false") boolean retomaVidrio,
                                 Principal principal, HttpSession session, RedirectAttributes ra) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";
        try {
            String direccionCompleta = direccionEnvio + ", " + ciudad;
            // T3: una obra urgente no puede esperar a una ruta consolidada
            boolean hayUrgente = obrasDelCarrito().stream().anyMatch(VidrioSostenible::isUrgente);
            Pedido pedido = pedidoService.crearPedidoDesdeCarrito(usuario, direccionCompleta, metodoPago, compensarCarbono,
                    empaqueReutilizable, entregaConsolidada && !hayUrgente, retomaVidrio);
            session.setAttribute("ultimoPedidoId", pedido.getId());
            return "redirect:/checkout/confirmacion/" + pedido.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al procesar el pedido: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "error");
            return "redirect:/checkout";
        }
    }

    /** CORREGIDO: solo el dueno del pedido ve la confirmacion (antes cualquier usuario logueado, por id). */
    @GetMapping("/confirmacion/{id}")
    public String verConfirmacion(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";
        return pedidoService.obtenerPorId(id)
                .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId()))
                .map(p -> {
                    model.addAttribute("pedido", p);
                    return "pedido-confirmacion";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("mensaje", "No tienes permisos para ver este pedido.");
                    ra.addFlashAttribute("tipoMensaje", "error");
                    return "redirect:/checkout/mis-pedidos";
                });
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(Principal principal, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(principal.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";
        List<Pedido> pedidos = pedidoService.obtenerPedidosUsuario(usuario);

        // Widget "Sostenible" (nombres reales de Pedido: huellaCarbono / compensacionCarbono)
        List<Pedido> compensados = pedidos.stream().filter(Pedido::isCompensacionCarbono).toList();
        double kgTotales = pedidos.stream().mapToDouble(Pedido::getHuellaCarbono).sum();
        double kgCompensados = compensados.stream().mapToDouble(Pedido::getHuellaCarbono).sum();
        double pctCompensado = kgTotales > 0 ? (kgCompensados / kgTotales) * 100.0 : 0.0;

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("kgTotales", redondear(kgTotales));
        model.addAttribute("kgCompensados", redondear(kgCompensados));
        model.addAttribute("pedidosCompensados", compensados.size());
        model.addAttribute("pctCompensado", Math.round(pctCompensado));
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

    private double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}