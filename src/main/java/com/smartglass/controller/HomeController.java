package com.smartglass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paginas de marketing puramente estaticas (sin datos del backend).
 * No necesitan VistaGlobalAttributesAdvice: index.html y acerca.html
 * no usan usuarioLogueado ni cantidadCarrito en su nav.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home() {
        return "index";
    }

    @GetMapping("/acerca")
    public String acerca() {
        return "acerca";
    }
}