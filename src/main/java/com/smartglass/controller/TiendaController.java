package com.smartglass.controller;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.ProductoService;
import com.smartglass.service.ReseñaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Paginas publicas de la vitrina (storefront): reemplaza las rutas
 * que tenia el viejo UsuarioController antes de separarlo en
 * AdminController (gestion interna) y AuthController (login/registro
 * por API). No requiere autenticacion -- SecurityConfig ya deja todo
 * excepto /admin, /usuario, /carrito, /checkout y publicar resenas
 * como permitAll.
 *
 * AJUSTE: rutas renombradas de /Tienda, /TiendaDestacada, /info/{id}
 * a /catalogo, /destacados, /detalle/{id} para coincidir con las
 * plantillas reales (catalogo.html, destacados.html, detalle.html).
 *
 * Los atributos de header (usuarioLogueado, usuario, cantidadCarrito)
 * los agrega VistaGlobalAttributesAdvice; este controller no repite
 * esa logica. El @ModelAttribute("usuario") en agregarResena reusa
 * ese mismo atributo -- SecurityConfig protege esa ruta especifica
 * como autenticada, asi que siempre llega resuelto.
 */
@Controller
public class TiendaController {

    private final ProductoService productoService;
    private final ReseñaService resenaService;

    public TiendaController(ProductoService productoService, ReseñaService resenaService) {
        this.productoService = productoService;
        this.resenaService = resenaService;
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
    public String verDetalleProducto(@PathVariable String id,
                                      @ModelAttribute(value = "usuario", binding = false) Usuario usuario,
                                      Model model) {
        return productoService.obtenerPorId(id)
                .map(producto -> {
                    double promedio = resenaService.calcularPromedio(id);
                    int estrellasLlenas = (int) Math.round(promedio);

                    model.addAttribute("producto", producto);
                    model.addAttribute("resenas", resenaService.obtenerPorProducto(id));
                    model.addAttribute("promedioCalificacion", promedio);
                    model.addAttribute("estrellasPromedio", "★".repeat(estrellasLlenas) + "☆".repeat(5 - estrellasLlenas));
                    if (usuario != null) {
                        resenaService.obtenerDeUsuario(id, usuario.getId())
                                .ifPresent(r -> model.addAttribute("miResena", r));
                    }
                    return "detalle";
                })
                .orElse("redirect:/catalogo");
    }

    @PostMapping("/detalle/{id}/resenas")
    public String agregarResena(@PathVariable String id,
                                 @RequestParam int calificacion,
                                 @RequestParam(required = false) String comentario,
                                 @ModelAttribute("usuario") Usuario usuario,
                                 RedirectAttributes ra) {
        try {
            resenaService.crearOActualizar(id, usuario.getId(), usuario.getNombre(), calificacion, comentario);
            ra.addFlashAttribute("mensaje", "¡Gracias por tu reseña!");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("mensaje", ex.getMessage());
            ra.addFlashAttribute("tipoMensaje", "error");
        }
        return "redirect:/detalle/" + id;
    }
}