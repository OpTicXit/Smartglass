package com.smartglass.controller;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.service.CarritoService;
import com.smartglass.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * AJUSTES: Producto -> CatalogoProducto; ProductoService.findByIdOrNull(...)
 * -> obtenerPorIdOrNull(...) (se renombro el metodo). Se quito la
 * inyeccion de UsuarioRepository: estaba declarada pero no se usaba
 * en ningun metodo de esta clase.
 */
@Controller
@RequestMapping("/carrito")
public class CarritoController {

    private final ProductoService productoService;
    private final CarritoService carritoService;

    public CarritoController(ProductoService productoService, CarritoService carritoService) {
        this.productoService = productoService;
        this.carritoService = carritoService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, Principal principal) {
        model.addAttribute("usuarioLogueado", principal != null);
        model.addAttribute("cantidadCarrito", carritoService.getCantidadTotal());
    }

    @GetMapping
    public String verCarrito(Model model) {
        model.addAttribute("items", carritoService.getItems());
        model.addAttribute("total", carritoService.getTotal());
        return "carrito";
    }

    @PostMapping("/agregar")
    public String agregarAlCarrito(@RequestParam String productoId,
        @RequestParam(defaultValue = "1") int cantidad,
        RedirectAttributes ra) {
        CatalogoProducto producto = productoService.obtenerPorIdOrNull(productoId);
        if (producto != null) {
            carritoService.agregarProducto(producto, cantidad);
            ra.addFlashAttribute("mensaje", "Producto añadido al carrito.");
            ra.addFlashAttribute("tipoMensaje", "success");
        }
        return "redirect:/carrito";
    }

    @PostMapping("/procesar-compra")
    public String procesarCompra(Principal principal) {
        if (principal == null) return "redirect:/login";
        return "redirect:/checkout";
    }

    @PostMapping("/actualizar")
    public String actualizarCantidad(@RequestParam String productoId, @RequestParam int cantidad) {
        carritoService.actualizarCantidad(productoId, cantidad);
        return "redirect:/carrito";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable String id) {
        carritoService.eliminarProducto(id);
        return "redirect:/carrito";
    }

    @PostMapping("/limpiar")
    public String limpiarCarrito() {
        carritoService.limpiar();
        return "redirect:/carrito";
    }
}