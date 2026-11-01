package com.smartglass.controller;

import com.smartglass.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard del usuario autenticado (usuario.html). Faltaba desde
 * que se separo el viejo UsuarioController en AdminController: sin
 * este controller, CustomSuccessHandler redirigia a "/usuario" y esa
 * ruta no existia (404 despues de cada login).
 *
 * El atributo "usuario" ya lo resuelve VistaGlobalAttributesAdvice
 * (agregado a su lista de controllers) -- este controller solo pone
 * lo que le es propio: los destacados a mostrar en el dashboard.
 */
@Controller
public class UsuarioDashboardController {

    private final ProductoService productoService;

    public UsuarioDashboardController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/usuario")
    public String dashboard(Model model) {
        model.addAttribute("destacados", productoService.obtenerDestacados());
        return "usuario";
    }
}