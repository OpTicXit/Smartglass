package com.smartglass.controller;

import com.smartglass.model.mongo.CatalogoProducto;
import com.smartglass.service.CarritoService;
import com.smartglass.service.ProductoService;
import com.smartglass.service.StockService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AJUSTES: Producto -> CatalogoProducto; ProductoService.findByIdOrNull(...)
 * -> obtenerPorIdOrNull(...) (se renombro el metodo). Se quito la
 * inyeccion de UsuarioRepository (no se usaba) y el @ModelAttribute
 * duplicado -- ahora lo resuelve VistaGlobalAttributesAdvice para
 * todos los controllers de vista.
 */
@Controller
@RequestMapping("/carrito")
public class CarritoController {

    private final ProductoService productoService;
    private final CarritoService carritoService;
    private final MessageSource messageSource;

    public CarritoController(ProductoService productoService, CarritoService carritoService,
                             MessageSource messageSource) {
        this.productoService = productoService;
        this.carritoService = carritoService;
        this.messageSource = messageSource;
    }

    /** T2: numeros como texto (D2: MessageFormat no los formatea segun el idioma). */
    private String texto(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    private int enCarrito(String productoId) {
        return carritoService.getItems().stream()
                .filter(i -> i.getProducto() != null && productoId.equals(i.getProducto().getId()))
                .mapToInt(com.smartglass.model.mongo.ItemCarrito::getCantidad).sum();
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
        if (producto != null && cantidad > 0) {
            // T2: el stock real limita lo que entra al carrito (se vuelve a comprobar al pagar)
            Integer disponibles = StockService.disponiblesParaAnadir(producto, enCarrito(productoId));
            if (producto.isAgotado()) {
                ra.addFlashAttribute("mensaje", texto("stock.carrito_agotado", producto.getNombre()));
                ra.addFlashAttribute("tipoMensaje", "error");
            } else if (disponibles != null && disponibles == 0) {
                ra.addFlashAttribute("mensaje", texto("stock.carrito_todas", producto.getNombre()));
                ra.addFlashAttribute("tipoMensaje", "error");
            } else if (disponibles != null && cantidad > disponibles) {
                carritoService.agregarProducto(producto, disponibles);
                ra.addFlashAttribute("mensaje", texto("stock.carrito_limitado", String.valueOf(disponibles), producto.getNombre()));
                ra.addFlashAttribute("tipoMensaje", "success");
            } else {
                carritoService.agregarProducto(producto, cantidad);
                ra.addFlashAttribute("mensaje", texto("flash.carrito_agregado"));
                ra.addFlashAttribute("tipoMensaje", "success");
            }
        }
        return "redirect:/carrito";
    }

    /**
     * T1 (D4): siempre a /checkout. Sin sesion, Spring Security guarda esa peticion y
     * manda a /login; tras autenticarse vuelve a /checkout con el carrito ya fusionado.
     */
    @PostMapping("/procesar-compra")
    public String procesarCompra() {
        return "redirect:/checkout";
    }

    @PostMapping("/actualizar")
    public String actualizarCantidad(@RequestParam String productoId, @RequestParam int cantidad,
                                     RedirectAttributes ra) {
        // T2: no se puede pedir mas de lo que hay (las obras a medida no tienen stock)
        CatalogoProducto producto = productoService.obtenerPorIdOrNull(productoId);
        if (producto != null && producto.isStockControlado() && cantidad > producto.getStock()) {
            int tope = Math.max(0, producto.getStock());
            ra.addFlashAttribute("mensaje", tope == 0
                    ? texto("stock.carrito_agotado", producto.getNombre())
                    : texto("stock.carrito_limitado", String.valueOf(tope), producto.getNombre()));
            ra.addFlashAttribute("tipoMensaje", "error");
            cantidad = tope;
        }
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