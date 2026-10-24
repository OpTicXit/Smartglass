package com.smartglass.controller;

import com.smartglass.dto.RegistroRequest;
import com.smartglass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Envoltorio MVC (formulario HTML) sobre AuthService.register(...).
 * AuthController ya expone el mismo registro como API REST
 * (POST /auth/registro, JSON); este controller es solo para registro.html.
 *
 * F43: @Valid + BindingResult activan los validadores del DTO y vuelven al
 * form con el primer mensaje de error en espanol. El honeypot redirige en
 * silencio a /login. El parametro ?limite lo pone RateLimitFilter (F41)
 * cuando se agota la cubeta de 10 registros/min por IP.
 */
@Controller
public class RegistroController {

    private final AuthService authService;

    public RegistroController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(@RequestParam(required = false) String limite, Model model) {
        if (limite != null) {
            model.addAttribute("error", "Demasiados intentos seguidos. Espera un minuto y vuelve a intentar.");
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute RegistroRequest request,
                            BindingResult errores,
                            Model model,
                            RedirectAttributes ra) {
        if (request.esSpam()) {
            return "redirect:/login"; // honeypot: rechazo silencioso
        }
        if (errores.hasErrors()) {
            model.addAttribute("error", errores.getAllErrors().get(0).getDefaultMessage());
            return "registro"; // Spring repuebla el form con el atributo "registroRequest"
        }
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