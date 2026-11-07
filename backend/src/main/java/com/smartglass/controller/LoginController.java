package com.smartglass.controller;

import com.smartglass.service.RecuperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

/**
 * SecurityConfig apunta a "/login" tanto en formLogin como en
 * oauth2Login, pero ningun controller la renderizaba -- sin esto,
 * cualquier redireccion a login (incluida la de un visitante que
 * intenta agregar algo al carrito) termina en 404.
 *
 * No arma atributos de Model: acceso.html (vista de login) lee "${param.error}" y
 * "${param.logout}" directamente de la query string, y "${mensaje}"
 * llega como flash attribute desde RegistroController -- no hace
 * falta duplicarlos aqui.
 * T1 (D4): unica excepcion, "paraPagar" = el visitante viene de /checkout
 * (peticion guardada por Spring Security) para avisarle que su carrito se conserva.
 */
@Controller
public class LoginController {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RecuperacionService recuperacion;

    public LoginController(RecuperacionService recuperacion) {
        this.recuperacion = recuperacion;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute("paraPagar", vieneDelPago(requestCache.getRequest(request, response)));
        model.addAttribute("recuperacionActiva", recuperacion.activa());
        model.addAttribute("vista", "login");
        return "acceso"; // nov. 2026: login y registro comparten la tarjeta de acceso.html
    }

    private static boolean vieneDelPago(SavedRequest guardada) {
        if (guardada == null) return false;
        try {
            String ruta = URI.create(guardada.getRedirectUrl()).getPath();
            return ruta != null && (ruta.equals("/checkout") || ruta.startsWith("/checkout/"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
