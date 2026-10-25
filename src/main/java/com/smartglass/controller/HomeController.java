package com.smartglass.controller;

import com.smartglass.service.EstadisticasPlataformaService;
import com.smartglass.service.Funcion;
import com.smartglass.service.PlanService;
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
    private final PlanService plan;

    public HomeController(EstadisticasPlataformaService estadisticas, PlanService plan) {
        this.estadisticas = estadisticas;
        this.plan = plan;
    }

    /** T5: las cifras en vivo son del plan Avanzado; sin ellas las vistas ocultan esos bloques. */
    private void cifras(Model model) {
        if (plan.activa(Funcion.CIFRAS_PUBLICAS)) model.addAttribute("plataforma", estadisticas.cifras());
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        cifras(model);
        return "index";
    }

    @GetMapping("/acerca")
    public String acerca(Model model) {
        cifras(model);
        return "acerca";
    }

    /** T4: guia de compra (version estatica del recorrido guiado; funciona sin JS). */
    @GetMapping("/guia")
    public String guia() {
        return "guia";
    }
}
