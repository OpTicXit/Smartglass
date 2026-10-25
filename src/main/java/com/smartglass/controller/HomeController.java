package com.smartglass.controller;

import com.smartglass.service.EstadisticasPlataformaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paginas de marketing. No necesitan VistaGlobalAttributesAdvice: index.html y acerca.html
 * no usan usuarioLogueado ni cantidadCarrito en su nav.
 * T4: inicio y acerca muestran cifras reales de la plataforma (EstadisticasPlataformaService).
 */
@Controller
public class HomeController {

    private final EstadisticasPlataformaService estadisticas;

    public HomeController(EstadisticasPlataformaService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("plataforma", estadisticas.cifras());
        return "index";
    }

    @GetMapping("/acerca")
    public String acerca(Model model) {
        model.addAttribute("plataforma", estadisticas.cifras());
        return "acerca";
    }

    /** T4: guia de compra (version estatica del recorrido guiado; funciona sin JS). */
    @GetMapping("/guia")
    public String guia() {
        return "guia";
    }
}
