package com.smartglass.controller;

import com.smartglass.dto.RegisterRequest;
import com.smartglass.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Envoltorio MVC (formulario HTML) sobre AuthService.register(...).
 * AuthController ya expone el mismo registro como API REST
 * (POST /auth/registro, JSON) para clientes que no usan formularios;
 * este controller es solo para registro.html.
 */
@Controller
public class RegistroController {

    private final AuthService authService;

    public RegistroController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/registro")
    public String mostrarFormulario() {
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute RegisterRequest request, Model model, RedirectAttributes ra) {
        try {
            authService.register(request);
            ra.addFlashAttribute("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "registro";
        }
    }
}