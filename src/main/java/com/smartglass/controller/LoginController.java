package com.smartglass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SecurityConfig apunta a "/login" tanto en formLogin como en
 * oauth2Login, pero ningun controller la renderizaba -- sin esto,
 * cualquier redireccion a login (incluida la de un visitante que
 * intenta agregar algo al carrito) termina en 404.
 *
 * No arma atributos de Model: login.html lee "${param.error}" y
 * "${param.logout}" directamente de la query string, y "${mensaje}"
 * llega como flash attribute desde RegistroController -- no hace
 * falta duplicarlos aqui.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}