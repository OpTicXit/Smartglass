package com.smartglass.controller;

import com.smartglass.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Paginas publicas de la vitrina (storefront): reemplaza las rutas
 * que tenia el viejo UsuarioController antes de separarlo en
 * AdminController (gestion interna) y AuthController (login/registro
 * por API). No requiere autenticacion -- SecurityConfig ya deja todo
 * excepto /admin, /usuario, /carrito y /checkout como permitAll.
 *
 * AJUSTE: rutas renombradas de /Tienda, /TiendaDestacada, /info/{id}
 * a /catalogo, /destacados, /detalle/{id} para coincidir con las
 * plantillas reales (catalogo.html, destacados.html, detalle.html).
 *
 * Los atributos de header (usuarioLogueado, usuario, cantidadCarrito)
 * los agrega VistaGlobalAttributesAdvice; este controller no repite
 * esa logica.
 */
@Controller
public class TiendaController {

    private final ProductoService productoService;

    public TiendaController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/catalogo")
    public String verCatalogo(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("productos", productoService.buscarPorNombre(q));
        model.addAttribute("query", q);
        return "catalogo";
    }

    @GetMapping("/destacados")
    public String verDestacados(Model model) {
        model.addAttribute("destacados", productoService.obtenerDestacados());
        return "destacados";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleProducto(@PathVariable String id, Model model) {
        return productoService.obtenerPorId(id)
                .map(producto -> {
                    model.addAttribute("producto", producto);
                    return "detalle";
                })
                .orElse("redirect:/catalogo");
    }
}